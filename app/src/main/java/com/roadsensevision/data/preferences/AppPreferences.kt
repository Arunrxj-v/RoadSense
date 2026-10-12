package com.roadsensevision.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * User-tunable detection configuration (persisted with DataStore).
 * Defaults match the design spec: confidence 0.75, NMS IoU 0.45,
 * 3-frame confirmation, 25 m merge radius, 30 min merge window.
 */
data class DetectionSettings(
    val confidenceThreshold: Float = 0.25f,
    val nmsIoUThreshold: Float = 0.45f,
    val confirmFrames: Int = 3,
    val mergeRadiusMeters: Float = 25f,
    val mergeWindowMs: Long = 30 * 60_000L,
    val inferenceThreads: Int = 4,
    val saveEvidence: Boolean = true,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "roadsense_settings")

class AppPreferences(private val context: Context) {

    private object Keys {
        val CONFIDENCE = floatPreferencesKey("confidence_threshold")
        val NMS_IOU = floatPreferencesKey("nms_iou_threshold")
        val CONFIRM_FRAMES = intPreferencesKey("confirm_frames")
        val MERGE_RADIUS = floatPreferencesKey("merge_radius_meters")
        val MERGE_WINDOW = longPreferencesKey("merge_window_ms")
        val THREADS = intPreferencesKey("inference_threads")
        val SAVE_EVIDENCE = booleanPreferencesKey("save_evidence")
    }

    val settings: Flow<DetectionSettings> = context.dataStore.data.map { prefs ->
        DetectionSettings(
            confidenceThreshold = prefs[Keys.CONFIDENCE] ?: 0.25f,
            nmsIoUThreshold = prefs[Keys.NMS_IOU] ?: 0.45f,
            confirmFrames = prefs[Keys.CONFIRM_FRAMES] ?: 3,
            mergeRadiusMeters = prefs[Keys.MERGE_RADIUS] ?: 25f,
            mergeWindowMs = prefs[Keys.MERGE_WINDOW] ?: 30 * 60_000L,
            inferenceThreads = prefs[Keys.THREADS] ?: 4,
            saveEvidence = prefs[Keys.SAVE_EVIDENCE] ?: true,
        )
    }

    suspend fun updateConfidenceThreshold(value: Float) {
        context.dataStore.edit { it[Keys.CONFIDENCE] = value.coerceIn(0.50f, 0.95f) }
    }

    suspend fun updateNmsIouThreshold(value: Float) {
        context.dataStore.edit { it[Keys.NMS_IOU] = value.coerceIn(0.10f, 0.90f) }
    }

    suspend fun updateInferenceThreads(value: Int) {
        context.dataStore.edit { it[Keys.THREADS] = value.coerceIn(1, 8) }
    }

    suspend fun updateSaveEvidence(value: Boolean) {
        context.dataStore.edit { it[Keys.SAVE_EVIDENCE] = value }
    }
}
