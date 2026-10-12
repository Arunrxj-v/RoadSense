package com.roadsensevision.monitoring

import android.app.Application
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roadsensevision.data.preferences.AppPreferences
import com.roadsensevision.data.preferences.DetectionSettings
import com.roadsensevision.data.repository.DetectionRepository
import com.roadsensevision.domain.detection.DetectionTracker
import com.roadsensevision.domain.models.ConfirmedDetection
import com.roadsensevision.domain.models.FrameDetection
import com.roadsensevision.domain.models.LocationQuality
import com.roadsensevision.domain.models.ProcessingStats
import com.roadsensevision.inference.PotholeDetector
import com.roadsensevision.location.GpsState
import com.roadsensevision.location.LocationProvider
import com.roadsensevision.location.LocationSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

enum class ModelStatus {
    LOADING, READY, ERROR
}

data class MonitoringUiState(
    val modelStatus: ModelStatus = ModelStatus.LOADING,
    val modelError: String? = null,
    val gpsState: GpsState = GpsState.DISABLED,
    val gpsAccuracy: Float? = null,
    val sessionActive: Boolean = false,
    val isPaused: Boolean = false,
    val sessionId: String? = null,
    val sessionStartMs: Long = 0L,
    val durationMs: Long = 0L,
    val detections: List<FrameDetection> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val confirmedCount: Int = 0,
    val inferenceLatencyMs: Float = 0f,
    val inferenceFps: Float = 0f,
    val lastDetectionAt: Long = 0L
)

class MonitoringViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AppPreferences(application)
    private val repository = DetectionRepository(application)
    private val detector = PotholeDetector(application)
    private val locationProvider = LocationProvider(application)
    private val tracker = DetectionTracker(minConfirmFrames = 3)

    private val _ui = MutableStateFlow(MonitoringUiState())
    val ui: StateFlow<MonitoringUiState> = _ui.asStateFlow()

    private val settingsLock = Any()
    private var settings: DetectionSettings = DetectionSettings(
        confidenceThreshold = 0.75f,
        nmsIoUThreshold = 0.45f,
        confirmFrames = 3,
        mergeRadiusMeters = 25f,
        mergeWindowMs = 30 * 60_000L,
        inferenceThreads = 4,
        saveEvidence = true
    )

    private var activeSessionId: String? = null
    private val inferenceBusy = AtomicBoolean(false)
    private var durationJob: Job? = null

    // Rolling performance measurement.
    private var latencyEma = 0f
    private val frameTimes = java.util.ArrayDeque<Long>()
    private var framesProcessed = 0L
    private var inferenceCount = 0L
    private var confirmedThisSession = 0

    init {
        observeSettings()
        initializeModel()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            preferences.settings.collect { newSettings ->
                val oldThreads = settings.inferenceThreads
                synchronized(settingsLock) { settings = newSettings }
                tracker.reset()
                // Thread count requires a session rebuild.
                if (oldThreads != newSettings.inferenceThreads && detector.isReady) {
                    detector.release()
                    initializeModel()
                }
            }
        }
    }

    private fun initializeModel() {
        _ui.update { it.copy(modelStatus = ModelStatus.LOADING, modelError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val threads = synchronized(settingsLock) { settings.inferenceThreads }
            val ok = detector.initialize(threads)
            _ui.update {
                if (ok) it.copy(modelStatus = ModelStatus.READY)
                else it.copy(
                    modelStatus = ModelStatus.ERROR,
                    modelError = detector.initializationError
                )
            }
        }
    }

    // ---------------- Session control ----------------

    fun startMonitoring() {
        if (_ui.value.sessionActive) return
        val state = _ui.value
        android.util.Log.i("RoadSenseV", "startMonitoring: sessionActive=${state.sessionActive}, modelStatus=${state.modelStatus}, detectorReady=${detector.isReady}, gpsState=${locationProvider.state}")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.i("RoadSenseV", "startMonitoring: launch Dispatchers.IO")

                val sessionId = repository.startSession()
                activeSessionId = sessionId
                confirmedThisSession = 0
                framesProcessed = 0
                inferenceCount = 0
                frameTimes.clear()
                locationProvider.start()
                tracker.reset()

                android.util.Log.i("RoadSenseV", "startMonitoring: about to init detector, isReady=${detector.isReady}")

                // Initialize the ONNX detector if not already ready.
                // This ensures onFrame() can process camera frames without dropping them.
                if (!detector.isReady) {
                    android.util.Log.i("RoadSenseV", "startMonitoring: initializing detector with threads=${settings.inferenceThreads}")
                    val ok = detector.initialize(settings.inferenceThreads)
                    android.util.Log.i("RoadSenseV", "startMonitoring: detector initialized ok=$ok, error=${detector.initializationError}, isReady=${detector.isReady}")
                } else {
                    android.util.Log.i("RoadSenseV", "startMonitoring: detector already ready, skipping init")
                }

                android.util.Log.i("RoadSenseV", "startMonitoring: about to update UI, sessionActive=true")

                durationJob?.cancel()
                durationJob = viewModelScope.launch {
                    val start = System.currentTimeMillis()
                    while (isActive && _ui.value.sessionActive) {
                        _ui.update { it.copy(durationMs = System.currentTimeMillis() - start) }
                        kotlinx.coroutines.delay(500)
                    }
                }

                _ui.update {
                    it.copy(
                        sessionActive = true,
                        isPaused = false,
                        sessionId = sessionId,
                        sessionStartMs = System.currentTimeMillis(),
                        durationMs = 0L,
                        confirmedCount = 0,
                        detections = emptyList()
                    )
                }
                android.util.Log.i("RoadSenseV", "startMonitoring: UI updated, sessionActive=true")
            } catch (e: Exception) {
                android.util.Log.e("RoadSenseV", "startMonitoring: EXCEPTION", e)
            }
        }
    }

    fun pauseMonitoring() {
        _ui.update { it.copy(isPaused = true) }
        tracker.reset()
    }

    fun resumeMonitoring() {
        _ui.update { it.copy(isPaused = false) }
        tracker.reset()
    }

    fun stopMonitoring() {
        val sessionId = activeSessionId
        viewModelScope.launch(Dispatchers.IO) {
            locationProvider.stop()
            durationJob?.cancel()
            if (sessionId != null) {
                val stats = currentStats()
                repository.endSession(sessionId, stats)
            }
            activeSessionId = null
            tracker.reset()
            // Preserve model and GPS state — only reset session-related fields.
            _ui.update {
                it.copy(
                    sessionActive = false,
                    isPaused = false,
                    sessionId = null,
                    detections = emptyList()
                )
            }
        }
    }

    private fun currentStats(): ProcessingStats {
        val fps = if (latencyEma > 0f) 1000f / latencyEma else 0f
        return ProcessingStats(
            framesProcessed = framesProcessed,
            inferenceCount = inferenceCount,
            avgLatencyMs = latencyEma,
            fps = fps
        )
    }

    // ---------------- Frame processing ----------------

    /**
     * Called on the CameraX analysis executor (background thread).
     * [bitmap] is the frame rotated to display orientation.
     */
    fun onFrame(bitmap: Bitmap, displayWidth: Int, displayHeight: Int, captureTimeMs: Long) {
        _ui.update { it.copy(imageWidth = displayWidth, imageHeight = displayHeight) }

        val state = _ui.value
        android.util.Log.i("RoadSenseV", "onFrame: sessionActive=${state.sessionActive}, isPaused=${state.isPaused}, detectorReady=${detector.isReady}, inferenceBusy=${inferenceBusy.get()}, bitmap_w=${bitmap.width}x${bitmap.height}")
        if (!state.sessionActive || state.isPaused || !detector.isReady) return
        if (!inferenceBusy.compareAndSet(false, true)) {
            android.util.Log.w("RoadSenseV", "onFrame: dropped stale frame, inferenceBusy was true")
            return // drop stale frame
        }

        try {
            val (confThreshold, iouThreshold) = synchronized(settingsLock) {
                settings.confidenceThreshold to settings.nmsIoUThreshold
            }
            android.util.Log.i("RoadSenseV", "onFrame: thresholds conf=$confThreshold iou=$iouThreshold")

            val preStartNs = System.nanoTime()
            val result = detector.detect(bitmap, confThreshold, iouThreshold)
            val preLatency = (System.nanoTime() - preStartNs) / 1_000_000f
            android.util.Log.i("RoadSenseV", "onFrame: detector.detect latency=${result.latencyMs}ms, detections=${result.detections.size}")

            inferenceCount++
            framesProcessed++

            // Rolling latency EMA.
            latencyEma = if (latencyEma == 0f) result.latencyMs
            else 0.9f * latencyEma + 0.1f * result.latencyMs

            android.util.Log.i("RoadSenseV", "onFrame: EMA latency=${latencyEma}ms, inferenceCount=$inferenceCount, framesProcessed=$framesProcessed")

            // Measured throughput over the last 2 seconds.
            frameTimes.addLast(captureTimeMs)
            val windowStart = captureTimeMs - 2000L
            while (frameTimes.isNotEmpty() && frameTimes.first() < windowStart) frameTimes.removeFirst()
            val measuredFps = if (frameTimes.size >= 2) {
                val span = (frameTimes.last() - frameTimes.first()) / 1000f
                if (span > 0f) frameTimes.size / span else 0f
            } else 0f

            android.util.Log.i("RoadSenseV", "onFrame: measuredFps=$measuredFps, latencyEma=${latencyEma}ms")

            val frameDetections = result.detections.map {
                FrameDetection(it.box, it.confidence, captureTimeMs)
            }

            // Temporal tracking + confirmation.
            val confirmed = tracker.update(frameDetections, captureTimeMs)

            android.util.Log.i("RoadSenseV", "onFrame: tracker.update kept=${confirmed.size} of ${frameDetections.size} candidates")

            _ui.update {
                it.copy(
                    detections = frameDetections,
                    confirmedCount = confirmedThisSession,
                    inferenceLatencyMs = latencyEma,
                    inferenceFps = measuredFps
                )
            }

            android.util.Log.i("RoadSenseV", "onFrame: UI updated, confirmedCount=${confirmedThisSession}, inferenceFps=$measuredFps")

            // Record each newly confirmed pothole.
            for (confirmedDetection in confirmed) {
                recordObservation(confirmedDetection, bitmap, captureTimeMs)
            }
            android.util.Log.i("RoadSenseV", "onFrame: recorded ${confirmed.size} observations")
        } catch (e: Exception) {
            android.util.Log.e("RoadSenseV", "onFrame: EXCEPTION", e)
        } finally {
            inferenceBusy.set(false)
        }
    }

    private fun recordObservation(
        confirmed: ConfirmedDetection,
        frame: Bitmap,
        frameTimeMs: Long
    ) {
        val sessionId = activeSessionId ?: return

        // Associate the GPS sample closest in time to the detection.
        val sample: LocationSample? = locationProvider.sampleNear(confirmed.confirmedAt)
        val quality = locationProvider.qualityOf(sample)

        // Save the representative (annotated) frame as evidence.
        val relativePath = synchronized(settingsLock) {
            if (settings.saveEvidence) {
                repository.saveEvidenceImage(frame, confirmed.box, confirmed.confidence)
            } else null
        }

        if (relativePath == null) {
            // Evidence save failed; do not record a hollow observation.
            android.util.Log.w("RoadSense", "Evidence save failed; observation dropped")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (mergeRadius, mergeWindow) = synchronized(settingsLock) {
                    settings.mergeRadiusMeters to settings.mergeWindowMs
                }
                val defectId = repository.recordObservation(
                    sessionId = sessionId,
                    confidence = confirmed.confidence,
                    observedAt = confirmed.confirmedAt,
                    latitude = sample?.latitude,
                    longitude = sample?.longitude,
                    gpsAccuracy = sample?.accuracyMeters,
                    locationQuality = quality,
                    evidenceRelativePath = relativePath,
                    mergeRadiusMeters = mergeRadius,
                    mergeWindowMs = mergeWindow
                )
                if (defectId != null) {
                    confirmedThisSession++
                    repository.incrementSessionCounters(sessionId, newDefect = true)
                    _ui.update {
                        it.copy(
                            confirmedCount = confirmedThisSession,
                            lastDetectionAt = confirmed.confirmedAt,
                            gpsState = locationProvider.state,
                            gpsAccuracy = sample?.accuracyMeters
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("RoadSense", "Failed to persist observation", e)
            }
        }
    }

    /** Refresh GPS status into UI state (called periodically from the UI layer). */
    fun refreshGpsStatus() {
        _ui.update {
            it.copy(
                gpsState = locationProvider.state,
                gpsAccuracy = locationProvider.lastSample?.accuracyMeters
            )
        }
    }

    fun getRepository(): DetectionRepository = repository

    override fun onCleared() {
        super.onCleared()
        durationJob?.cancel()
        locationProvider.stop()
        detector.release()
    }
}
