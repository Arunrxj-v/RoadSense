package com.roadsensevision.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.roadsensevision.data.database.AppDatabase
import com.roadsensevision.data.database.MonitoringSessionEntity
import com.roadsensevision.data.database.PotholeObservationEntity
import com.roadsensevision.data.database.RoadDefectEntity
import com.roadsensevision.data.database.dao.MonitoringSessionDao
import com.roadsensevision.data.database.dao.PotholeObservationDao
import com.roadsensevision.data.database.dao.RoadDefectDao
import com.roadsensevision.data.sync.SyncScheduler
import com.roadsensevision.domain.detection.DuplicatePolicy
import com.roadsensevision.domain.models.BoxF
import com.roadsensevision.domain.models.LocationQuality
import com.roadsensevision.domain.models.ProcessingStats
import com.roadsensevision.domain.models.SessionStatus
import com.roadsensevision.domain.models.SyncStatus
import com.roadsensevision.utilities.GeoUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

class DetectionRepository(private val appContext: Context) {

    private val db: AppDatabase by lazy { AppDatabase.getInstance(appContext) }

    val sessionDao: MonitoringSessionDao get() = db.monitoringSessionDao()
    val defectDao: RoadDefectDao get() = db.roadDefectDao()
    val observationDao: PotholeObservationDao get() = db.potholeObservationDao()

    // ---- Defect ID generation (stable, human-readable PH-00001...) ----
    private var defectCounter: Int = -1
    private val defectIdMutex = Mutex()

    private suspend fun nextDefectId(): String = defectIdMutex.withLock {
        if (defectCounter < 0) {
            defectCounter = defectDao.getDefectCount()
        }
        defectCounter++
        "PH-%05d".format(defectCounter)
    }

    // ---- Evidence storage ----
    /** Returns the app-private evidence directory. */
    private val evidenceDir: File by lazy {
        File(appContext.filesDir, "evidence").apply { mkdirs() }
    }

    /**
     * Save a representative (annotated) frame as evidence.
     * Large frames are downscaled before annotation to bound
     * memory and storage usage.
     * Returns the relative path within app-private storage.
     */
    fun saveEvidenceImage(frame: Bitmap, box: BoxF, confidence: Float): String? {
        return try {
            val scaled = downscaleForEvidence(frame)
            // Re-map the box into the scaled image space.
            val scaleX = scaled.width.toFloat() / frame.width
            val scaleY = scaled.height.toFloat() / frame.height
            val scaledBox = android.graphics.RectF(
                box.left * scaleX, box.top * scaleY,
                box.right * scaleX, box.bottom * scaleY
            )
            val annotated = drawDetectionOverlay(scaled, scaledBox, confidence)
            val name = "obs_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val file = File(evidenceDir, name)
            FileOutputStream(file).use { out ->
                annotated.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            "evidence/$name"
        } catch (e: Exception) {
            null
        }
    }

    private fun downscaleForEvidence(frame: Bitmap): Bitmap {
        val maxEdge = 1600
        val long = maxOf(frame.width, frame.height)
        return if (long <= maxEdge) frame else {
            val scale = maxEdge.toFloat() / long
            Bitmap.createScaledBitmap(
                frame,
                (frame.width * scale).roundToInt(),
                (frame.height * scale).roundToInt(),
                true
            )
        }
    }

    private fun drawDetectionOverlay(frame: Bitmap, box: android.graphics.RectF, confidence: Float): Bitmap {
        val out = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(frame, 0f, 0f, null)
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            color = 0xFFEA580C.toInt()
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawRect(box, paint)
        // Label tag
        val label = "Pothole · ${(confidence * 100).roundToInt()}%"
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 42f
            isAntiAlias = true
            setShadowLayer(2f, 0f, 1f, Color.BLACK)
        }
        val tagPaint = Paint().apply { color = 0xFFCC4900.toInt() }
        val textWidth = textPaint.measureText(label)
        val padH = 20f
        val padV = 12f
        val tagLeft = box.left
        val tagTop = box.top - 64f
        canvas.drawRect(tagLeft, tagTop, tagLeft + textWidth + padH * 2, box.top - 4f, tagPaint)
        canvas.drawText(label, tagLeft + padH, box.top - 24f, textPaint)
        return out
    }

    fun loadEvidence(relativePath: String): Bitmap? = try {
        val file = File(appContext.filesDir, relativePath)
        android.graphics.BitmapFactory.decodeFile(file.absolutePath)
    } catch (e: Exception) {
        null
    }

    /** Storage used by evidence images, in bytes. */
    fun evidenceStorageBytes(): Long =
        evidenceDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    // ---- Session management ----
    suspend fun startSession(): String {
        val sessionId = "RS-${System.currentTimeMillis().toString(36).uppercase()}"
        sessionDao.insert(
            MonitoringSessionEntity(
                sessionId = sessionId,
                startedAt = System.currentTimeMillis(),
                endedAt = null,
                status = SessionStatus.ACTIVE.value,
                observationCount = 0,
                uniqueDefectCount = 0,
                processingStatistics = "{}"
            )
        )
        return sessionId
    }

    suspend fun endSession(sessionId: String, stats: ProcessingStats) {
        val session = sessionDao.getSession(sessionId) ?: return
        sessionDao.update(
            session.copy(
                endedAt = System.currentTimeMillis(),
                status = SessionStatus.COMPLETED.value,
                processingStatistics = stats.toJson()
            )
        )
    }

    suspend fun updateSessionStats(sessionId: String, stats: ProcessingStats) {
        val session = sessionDao.getSession(sessionId) ?: return
        if (session.status == SessionStatus.ACTIVE.value) {
            sessionDao.update(session.copy(processingStatistics = stats.toJson()))
        }
    }

    suspend fun incrementSessionCounters(sessionId: String, newDefect: Boolean) {
        val session = sessionDao.getSession(sessionId) ?: return
        sessionDao.update(
            session.copy(
                observationCount = session.observationCount + 1,
                uniqueDefectCount = session.uniqueDefectCount + if (newDefect) 1 else 0
            )
        )
    }

    // ---- Observation recording with duplicate prevention ----
    /**
     * Record a confirmed detection as an observation.
     *
     * Duplicate prevention: an existing [RoadDefect] is reused when a prior
     * defect was observed within [mergeWindowMs] and within a distance that
     * accounts for GPS accuracy (mergeRadiusMeters + accuracy allowance).
     * GPS proximity alone is never treated as definitive proof — the time
     * window must also hold.
     *
     * Returns the defectId the observation was attached to, or null on failure.
     */
    suspend fun recordObservation(
        sessionId: String,
        confidence: Float,
        observedAt: Long,
        latitude: Double?,
        longitude: Double?,
        gpsAccuracy: Float?,
        locationQuality: LocationQuality,
        evidenceRelativePath: String?,
        mergeRadiusMeters: Float,
        mergeWindowMs: Long
    ): String? {
        // Find a matching existing defect (nearby AND recently observed).
        var matchedDefect: RoadDefectEntity? = null
        if (latitude != null && longitude != null) {
            val candidates = defectDao.toList()
            for (defect in candidates) {
                val dLat = defect.latestLatitude ?: continue
                val dLon = defect.latestLongitude ?: continue
                val distance = GeoUtils.distanceMeters(latitude, longitude, dLat, dLon)
                val timeDelta = observedAt - defect.lastObservedAt
                if (DuplicatePolicy.shouldMerge(
                        distanceMeters = distance,
                        mergeRadiusMeters = mergeRadiusMeters,
                        accuracyA = gpsAccuracy,
                        accuracyB = defect.latestGpsAccuracy,
                        timeDeltaMs = timeDelta,
                        mergeWindowMs = mergeWindowMs,
                    )
                ) {
                    matchedDefect = defect
                    break
                }
            }
        }

        val defectId: String
        if (matchedDefect != null) {
            defectId = matchedDefect.defectId
            defectDao.update(
                matchedDefect.copy(
                    lastObservedAt = observedAt,
                    latestLatitude = latitude ?: matchedDefect.latestLatitude,
                    latestLongitude = longitude ?: matchedDefect.latestLongitude,
                    latestGpsAccuracy = gpsAccuracy ?: matchedDefect.latestGpsAccuracy,
                    observationCount = matchedDefect.observationCount + 1
                )
            )
        } else {
            defectId = nextDefectId()
            defectDao.insert(
                RoadDefectEntity(
                    defectId = defectId,
                    firstDetectedAt = observedAt,
                    lastObservedAt = observedAt,
                    latestLatitude = latitude,
                    latestLongitude = longitude,
                    latestGpsAccuracy = gpsAccuracy,
                    observationCount = 1,
                    status = "ACTIVE",
                    lastVerifiedAt = null
                )
            )
        }

        val observation = PotholeObservationEntity(
            observationId = UUID.randomUUID().toString(),
            defectId = defectId,
            sessionId = sessionId,
            observedAt = observedAt,
            latitude = latitude,
            longitude = longitude,
            gpsAccuracy = gpsAccuracy,
            confidence = confidence,
            imageRelativePath = evidenceRelativePath,
            locationQuality = locationQuality.name,
            synchronizationStatus = if (latitude != null && longitude != null) {
                SyncStatus.PENDING.value
            } else {
                SyncStatus.FAILED.value
            },
            serverRecordId = null
        )
        observationDao.insert(observation)
        if (observation.synchronizationStatus == SyncStatus.PENDING.value) {
            SyncScheduler.requestImmediateSync(appContext)
        }
        return defectId
    }

    suspend fun getObservationsAwaitingSync(limit: Int): List<PotholeObservationEntity> =
        observationDao.getAwaitingSync(limit)

    suspend fun markSynced(observationId: String, serverRecordId: String) {
        observationDao.updateSyncState(
            observationId = observationId,
            status = SyncStatus.SYNCED.value,
            serverRecordId = serverRecordId,
        )
    }

    suspend fun markSyncFailed(observationId: String) {
        observationDao.updateSyncState(
            observationId = observationId,
            status = SyncStatus.FAILED.value,
            serverRecordId = null,
        )
    }

    fun observeSyncCount(status: SyncStatus): Flow<Int> =
        observationDao.observeCountByStatus(status.value)

    fun observeFailedUploadCount(): Flow<Int> = observationDao.observeFailedUploadCount()

    fun observeNoLocationCount(): Flow<Int> = observationDao.observeNoLocationCount()

    suspend fun getLatestDefects(limit: Int): List<RoadDefectEntity> =
        defectDao.getRecentDefects(limit)

    suspend fun getDefect(defectId: String): RoadDefectEntity? = defectDao.getDefect(defectId)

    suspend fun getObservationsForDefect(defectId: String): List<PotholeObservationEntity> =
        observationDao.observeObservationsForDefect(defectId).first()

    fun observeDefects(): Flow<List<RoadDefectEntity>> = defectDao.observeDefects()
    fun observeObservations(): Flow<List<PotholeObservationEntity>> = observationDao.observeObservations()
    fun observeObservationsForDefect(defectId: String): Flow<List<PotholeObservationEntity>> =
        observationDao.observeObservationsForDefect(defectId)
    fun observeDefectCount(): Flow<Int> = defectDao.observeDefectCount()
    fun observeObservationCount(): Flow<Int> = observationDao.observeObservationCount()
    fun observeSessions(): Flow<List<MonitoringSessionEntity>> = sessionDao.observeSessions()
    fun observeLatestSession(): Flow<MonitoringSessionEntity?> = sessionDao.observeLatestSession()

    suspend fun clearAllData() {
        db.clearAllTables()
        evidenceDir.walkTopDown().filter { it.isFile }.forEach { it.delete() }
    }
}

private fun ProcessingStats.toJson(): String =
    "{\"framesProcessed\":$framesProcessed,\"inferenceCount\":$inferenceCount,\"avgLatencyMs\":$avgLatencyMs,\"fps\":$fps}"

// Synchronous list variants used inside recordObservation (called off the main thread).
private suspend fun RoadDefectDao.toList(): List<RoadDefectEntity> = observeDefects().first()

private suspend fun PotholeObservationDao.toListForDefect(defectId: String): List<PotholeObservationEntity> =
    observeObservationsForDefect(defectId).first()
