package com.roadsensevision.inference

import com.roadsensevision.domain.models.BoxF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NMS tests — the packaged graph has no NMS node, so this is the code that
 * decides how many potholes are reported per frame.
 */
class NmsTest {

    @Test
    fun `identical boxes have IoU of one`() {
        val a = BoxF(0f, 0f, 100f, 100f)
        assertEquals(1f, Nms.iou(a, a), 1e-5f)
    }

    @Test
    fun `disjoint boxes have IoU of zero`() {
        val a = BoxF(0f, 0f, 100f, 100f)
        val b = BoxF(200f, 200f, 300f, 300f)
        assertEquals(0f, Nms.iou(a, b), 1e-6f)
    }

    @Test
    fun `half-overlapping boxes produce the expected IoU`() {
        val a = BoxF(0f, 0f, 100f, 100f)
        val b = BoxF(0f, 50f, 100f, 150f)   // intersection 100x50 = 5000, union 15000
        assertEquals(5000f / 15000f, Nms.iou(a, b), 1e-5f)
    }

    @Test
    fun `suppress removes lower-scoring duplicates`() {
        val a = ScoredBox(BoxF(0f, 0f, 100f, 100f), 0.9f)
        val b = ScoredBox(BoxF(5f, 5f, 105f, 105f), 0.7f)   // heavy overlap
        val kept = Nms.suppress(listOf(b, a), iouThreshold = 0.45f)
        assertEquals(1, kept.size)
        assertEquals(0.9f, kept[0].score, 1e-6f)
    }

    @Test
    fun `suppress keeps spatially distinct boxes`() {
        val a = ScoredBox(BoxF(0f, 0f, 100f, 100f), 0.9f)
        val b = ScoredBox(BoxF(300f, 300f, 400f, 400f), 0.6f)
        val kept = Nms.suppress(listOf(a, b), iouThreshold = 0.45f)
        assertEquals(2, kept.size)
    }

    @Test
    fun `suppress returns survivors sorted by descending score`() {
        val boxes = listOf(
            ScoredBox(BoxF(0f, 0f, 50f, 50f), 0.55f),
            ScoredBox(BoxF(10f, 10f, 60f, 60f), 0.80f),   // overlaps 0 -> suppressed
            ScoredBox(BoxF(400f, 0f, 450f, 50f), 0.70f),
            ScoredBox(BoxF(0f, 400f, 50f, 450f), 0.95f),
        )
        val kept = Nms.suppress(boxes, iouThreshold = 0.45f)
        assertEquals(3, kept.size)
        // NMS keeps the higher-scoring box of each overlapping pair, so the
        // 0.80 box survives over the 0.55 box it overlaps (IoU 0.47).
        val scores = kept.map { it.score }
        assertEquals(listOf(0.95f, 0.80f, 0.70f), scores)
        assertTrue(scores.zipWithNext().all { (a, b) -> a >= b })
    }

    @Test
    fun `single candidate passes through untouched`() {
        val only = ScoredBox(BoxF(1f, 2f, 3f, 4f), 0.42f)
        val kept = Nms.suppress(listOf(only), iouThreshold = 0.45f)
        assertEquals(1, kept.size)
        assertEquals(only, kept[0])
    }
}
