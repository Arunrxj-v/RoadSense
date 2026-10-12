package com.roadsensevision.domain.detection

import com.roadsensevision.domain.models.BoxF
import com.roadsensevision.domain.models.ConfirmedDetection
import com.roadsensevision.domain.models.FrameDetection
import com.roadsensevision.inference.Nms

/**
 * Tracks frame-level detections across frames and decides when a detection
 * is confirmed and safe to record as an observation.
 *
 * Responsibilities:
 *  - match detections between consecutive frames by box overlap (IoU), giving
 *    each physical pothole a stable track id;
 *  - require [minConfirmFrames] confirmations before a track is emitted
 *    (temporal confirmation — a single flicker frame never becomes a record);
 *  - emit at most one confirmation per track per [cooldownMs] so the same
 *    pothole does not flood the log;
 *  - expire tracks missed for several consecutive frames.
 *
 * GPS is intentionally NOT used here — the repository applies the separate,
 * accuracy-aware [DuplicatePolicy] when associating an observation with a
 * defect.
 *
 * Pure Kotlin (no Android types) so the confirmation behaviour is unit-testable.
 */
class DetectionTracker(
    private val minConfirmFrames: Int = 3,
    private val matchIouThreshold: Float = 0.30f,
    private val cooldownMs: Long = 20_000L,
    private val maxMissFrames: Int = 12,
) {

    private class Track(
        val id: Int,
        var box: BoxF,
        var confidence: Float,
        var lastSeenAt: Long,
        var confirmations: Int = 1,
        var emittedAt: Long = 0L,
        var misses: Int = 0,
    )

    private val tracks = LinkedHashMap<Int, Track>()
    private var nextTrackId = 1

    /**
     * Processes one frame of detections.
     *
     * @param frameDetections raw (confidence/NMS filtered) detections for this frame
     * @param frameTimeMs     capture timestamp of the frame, in wall-clock ms
     * @return detections that just became confirmed and are due for recording
     */
    fun update(
        frameDetections: List<FrameDetection>,
        frameTimeMs: Long
    ): List<ConfirmedDetection> {
        val matched = BooleanArray(frameDetections.size)

        // Greedy IoU matching of existing tracks against this frame.
        for (track in tracks.values) {
            var bestIdx = -1
            var bestIou = matchIouThreshold
            for (i in frameDetections.indices) {
                if (matched[i]) continue
                val iou = Nms.iou(track.box, frameDetections[i].box)
                if (iou > bestIou) {
                    bestIou = iou
                    bestIdx = i
                }
            }
            if (bestIdx >= 0) {
                matched[bestIdx] = true
                val detection = frameDetections[bestIdx]
                track.box = detection.box
                track.confidence = maxOf(track.confidence, detection.confidence)
                track.lastSeenAt = frameTimeMs
                track.confirmations++
                track.misses = 0
            } else {
                track.misses++
            }
        }

        // Unmatched detections start new tracks.
        for (i in frameDetections.indices) {
            if (matched[i]) continue
            val detection = frameDetections[i]
            tracks[nextTrackId] = Track(
                id = nextTrackId,
                box = detection.box,
                confidence = detection.confidence,
                lastSeenAt = frameTimeMs,
            )
            nextTrackId++
        }

        // Expire tracks missed for several consecutive frames.
        tracks.values.removeAll { it.misses >= maxMissFrames }

        // Emit newly confirmed detections (one per track, then cooldown).
        val confirmed = mutableListOf<ConfirmedDetection>()
        for (track in tracks.values) {
            if (track.confirmations < minConfirmFrames) continue
            if (track.emittedAt != 0L && frameTimeMs - track.emittedAt < cooldownMs) continue
            track.emittedAt = frameTimeMs
            confirmed.add(
                ConfirmedDetection(
                    box = track.box,
                    confidence = track.confidence,
                    confirmedAt = track.lastSeenAt,
                )
            )
        }
        return confirmed
    }

    fun reset() {
        tracks.clear()
        nextTrackId = 1
    }

    /** Number of tracks currently alive (for diagnostics/tests). */
    val activeTrackCount: Int get() = tracks.size

    companion object {
        /** IoU convenience delegate to the shared implementation. */
        fun iou(a: BoxF, b: BoxF): Float = Nms.iou(a, b)
    }
}
