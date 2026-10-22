package com.roadsensevision.domain.models

/**
 * Pure axis-aligned bounding box.
 *
 * Deliberately avoids `android.graphics.RectF` so that detection, tracking,
 * NMS and duplicate-prevention logic are plain Kotlin and unit-testable on
 * the JVM without the Android framework. UI/Canvas code converts at the edge.
 */
data class BoxF(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
}

/**
 * Frame-level detection produced by the model for a single camera frame.
 * Coordinates are in display-orientation image pixels (already un-letterboxed
 * back into the source frame space), so they can be mapped onto the preview
 * view directly.
 */
data class FrameDetection(
    val box: BoxF,
    val confidence: Float,
    val captureTimeMs: Long,
    val className: String = "pothole",
)

/**
 * A detection that has been stable across [minConfirmFrames] consecutive
 * frames and is safe to record as a persistent observation. This is the
 * boundary between "frame detections" (transient) and "observations"
 * (persisted with GPS + evidence).
 */
data class ConfirmedDetection(
    val box: BoxF,
    val confidence: Float,
    val confirmedAt: Long,
)

/**
 * Measured inference performance for a monitoring session.
 * Serialized into [com.roadsensevision.data.database.MonitoringSessionEntity.processingStatistics].
 */
data class ProcessingStats(
    val framesProcessed: Long,
    val inferenceCount: Long,
    val avgLatencyMs: Float,
    val fps: Float,
)

/**
 * Severity is derived from model confidence (documented heuristic, Phase 1):
 * Severe >= 0.85, Moderate >= 0.70, otherwise Low.
 */
enum class Severity(val label: String) {
    LOW("Low"),
    MODERATE("Moderate"),
    SEVERE("Severe");

    companion object {
        fun fromConfidence(confidence: Float): Severity = when {
            confidence >= 0.85f -> SEVERE
            confidence >= 0.70f -> MODERATE
            else -> LOW
        }
    }
}

/**
 * GPS quality of the sample associated with an observation.
 * UNAVAILABLE means no fix fell inside the association window —
 * coordinates are never invented.
 */
enum class LocationQuality {
    GOOD,      // accuracy <= 5 m
    FAIR,      // accuracy <= 15 m
    POOR,      // accuracy > 15 m
    UNAVAILABLE
}

/** Upload lifecycle for a locally persisted observation. */
enum class SyncStatus(val value: String) {
    PENDING("PENDING"),
    SYNCED("SYNCED"),
    FAILED("FAILED"),
}

/** Monitoring session lifecycle state. */
enum class SessionStatus(val value: String) {
    ACTIVE("ACTIVE"),
    COMPLETED("COMPLETED"),
    ABORTED("ABORTED"),
}
