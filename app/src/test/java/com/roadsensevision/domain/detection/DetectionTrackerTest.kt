package com.roadsensevision.domain.detection

import com.roadsensevision.domain.models.BoxF
import com.roadsensevision.domain.models.FrameDetection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Temporal confirmation tests: a single flicker frame must never become a
 * record, and one physical pothole must not flood the log.
 */
class DetectionTrackerTest {

    private fun box(x: Float, y: Float, size: Float = 100f) =
        BoxF(x, y, x + size, y + size)

    private fun detection(b: BoxF, score: Float = 0.9f, t: Long = 0L) =
        FrameDetection(box = b, confidence = score, captureTimeMs = t)

    @Test
    fun `nothing is confirmed before the required number of frames`() {
        val tracker = DetectionTracker(minConfirmFrames = 3)
        assertTrue(tracker.update(listOf(detection(box(10f, 10f))), 0L).isEmpty())
        assertTrue(tracker.update(listOf(detection(box(10f, 10f))), 100L).isEmpty())
    }

    @Test
    fun `confirmation fires on the third matching frame`() {
        val tracker = DetectionTracker(minConfirmFrames = 3)
        tracker.update(listOf(detection(box(10f, 10f), score = 0.70f)), 0L)
        tracker.update(listOf(detection(box(11f, 10f), score = 0.85f)), 100L)
        val confirmed = tracker.update(listOf(detection(box(12f, 11f), score = 0.88f)), 200L)

        assertEquals(1, confirmed.size)
        assertEquals(0.88f, confirmed[0].confidence, 1e-6f)
        assertEquals(200L, confirmed[0].confirmedAt)
    }

    @Test
    fun `a flickering detection never reaches confirmation`() {
        val tracker = DetectionTracker(minConfirmFrames = 3)
        var t = 0L
        // Seen once, gone for many frames, seen once again — never 3 in a row.
        tracker.update(listOf(detection(box(50f, 50f))), t); t += 100
        repeat(20) { tracker.update(emptyList(), t); t += 100 }
        assertTrue(tracker.update(listOf(detection(box(50f, 50f))), t).isEmpty())
        assertEquals(1, tracker.activeTrackCount)
    }

    @Test
    fun `cooldown prevents the same pothole from being recorded repeatedly`() {
        val tracker = DetectionTracker(minConfirmFrames = 3, cooldownMs = 20_000L)
        var t = 0L
        var firstCount = 0
        repeat(3) {
            firstCount = tracker.update(listOf(detection(box(10f, 10f))), t).size
            t += 100
        }

        // Confirmed exactly once, on the third matching frame.
        assertEquals(1, firstCount)

        // Still in view, inside the cooldown -> silent.
        assertTrue(tracker.update(listOf(detection(box(10f, 10f))), t).isEmpty())
        t = 5_000L
        assertTrue(tracker.update(listOf(detection(box(10f, 10f))), t).isEmpty())

        // Confirmed again only once the cooldown elapses.
        t = 21_000L
        val second = tracker.update(listOf(detection(box(10f, 10f))), t)
        assertEquals(1, second.size)
    }

    @Test
    fun `two separate potholes become two independent tracks`() {
        val tracker = DetectionTracker(minConfirmFrames = 3)
        var t = 0L
        var confirmedCount = 0
        repeat(3) {
            confirmedCount += tracker.update(
                listOf(detection(box(10f, 10f)), detection(box(500f, 500f))),
                t
            ).size
            t += 100
        }
        assertEquals(2, confirmedCount)
        assertEquals(2, tracker.activeTrackCount)
    }

    @Test
    fun `reset clears confirmation progress`() {
        val tracker = DetectionTracker(minConfirmFrames = 3)
        repeat(3) { tracker.update(listOf(detection(box(10f, 10f))), it * 100L) }
        assertEquals(1, tracker.activeTrackCount)

        tracker.reset()
        assertEquals(0, tracker.activeTrackCount)
        assertTrue(tracker.update(listOf(detection(box(10f, 10f))), 1_000L).isEmpty())
    }

    @Test
    fun `the highest confidence seen while tracking is reported`() {
        val tracker = DetectionTracker(minConfirmFrames = 3)
        tracker.update(listOf(detection(box(10f, 10f), score = 0.60f)), 0L)
        tracker.update(listOf(detection(box(10f, 10f), score = 0.95f)), 100L)
        val confirmed = tracker.update(listOf(detection(box(10f, 10f), score = 0.70f)), 200L)
        assertEquals(1, confirmed.size)
        assertEquals(0.95f, confirmed[0].confidence, 1e-6f)
    }
}
