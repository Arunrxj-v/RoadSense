package com.roadsensevision.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roadsensevision.data.preferences.AppPreferences
import com.roadsensevision.data.preferences.DetectionSettings
import com.roadsensevision.data.repository.DetectionRepository
import com.roadsensevision.data.database.MonitoringSessionEntity
import com.roadsensevision.data.database.PotholeObservationEntity
import com.roadsensevision.data.database.RoadDefectEntity
import com.roadsensevision.data.sync.IngestionClient
import com.roadsensevision.data.sync.SyncScheduler
import com.roadsensevision.data.sync.SyncSummary
import com.roadsensevision.domain.models.SyncStatus
import com.roadsensevision.utilities.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Repository-backed state shared across non-live screens. */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    val repository = DetectionRepository(application)
    val preferences = AppPreferences(application)

    val defects: StateFlow<List<RoadDefectEntity>> = repository.observeDefects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val observations: StateFlow<List<PotholeObservationEntity>> = repository.observeObservations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestSession: StateFlow<MonitoringSessionEntity?> = repository.observeLatestSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val defectCount: StateFlow<Int> = repository.observeDefectCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val observationCount: StateFlow<Int> = repository.observeObservationCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncSummary: StateFlow<SyncSummary> = combine(
        repository.observeSyncCount(SyncStatus.PENDING),
        repository.observeSyncCount(SyncStatus.SYNCED),
        repository.observeFailedUploadCount(),
        repository.observeNoLocationCount(),
        SyncScheduler.uploading,
    ) { pending, synced, failed, noLocation, uploading ->
        SyncSummary(
            pending = pending,
            synced = synced,
            failed = failed,
            noLocation = noLocation,
            configured = IngestionClient().isConfigured,
            uploading = uploading,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SyncSummary(configured = IngestionClient().isConfigured),
    )

    val settings: StateFlow<DetectionSettings> = preferences.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, DetectionSettings(
            confidenceThreshold = 0.75f,
            nmsIoUThreshold = 0.45f,
            confirmFrames = 3,
            mergeRadiusMeters = 25f,
            mergeWindowMs = 30 * 60_000L,
            inferenceThreads = 4,
            saveEvidence = true
        ))

    /** Cumulative road distance monitored (sum of observation coordinates). */
    val distanceMeters: StateFlow<Double> = combine(observations) { (obs) ->
        if (obs.size < 2) 0.0
        else obs.sortedBy { it.observedAt }
            .zipWithNext { a, b ->
                if (a.latitude != null && a.longitude != null && b.latitude != null && b.longitude != null) {
                    GeoUtils.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
                } else 0.0
            }
            .sum()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun loadEvidence(relativePath: String): Bitmap? = repository.loadEvidence(relativePath)

    fun evidenceStorageBytes(): Long = repository.evidenceStorageBytes()

    fun updateConfidence(value: Float) {
        viewModelScope.launch { preferences.updateConfidenceThreshold(value) }
    }

    fun updateInferenceThreads(value: Int) {
        viewModelScope.launch { preferences.updateInferenceThreads(value) }
    }

    fun updateSaveEvidence(value: Boolean) {
        viewModelScope.launch { preferences.updateSaveEvidence(value) }
    }

    fun syncNow() {
        SyncScheduler.requestImmediateSync(getApplication<Application>(), force = true)
    }

    fun clearAllData(onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData()
            launch { onDone() }
        }
    }

    /** Latest defect for the dashboard "Recent detection" card. */
    fun latestDefect(): RoadDefectEntity? = defects.value.maxByOrNull { it.lastObservedAt }

    fun observationForDefect(defectId: String): PotholeObservationEntity? =
        observations.value.filter { it.defectId == defectId }.maxByOrNull { it.confidence }

    fun defectsInOrder(): List<RoadDefectEntity> = defects.value.sortedByDescending { it.lastObservedAt }

    fun formatKm(meters: Double): String {
        val km = meters / 1000.0
        return String.format(java.util.Locale.US, "%.1f km", km)
    }
}
