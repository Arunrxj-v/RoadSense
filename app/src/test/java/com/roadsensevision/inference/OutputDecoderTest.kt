package com.roadsensevision.inference

import com.roadsensevision.domain.models.BoxF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decode tests for the raw `output0` tensor: [1, 5, 8400] laid out as
 * cx, cy, w, h, score in 640-space, then un-letterboxed to source pixels.
 */
class OutputDecoderTest {

    private val transform = LetterboxTransform(size = 640, scale = 0.5f, padX = 0f, padY = 140f)

    private fun syntheticOutput(): Array<Array<FloatArray>> =
        Array(1) { Array(5) { FloatArray(8400) } }

    @Test
    fun `decodes cx cy w h and un-letterboxes to source pixels`() {
        val raw = syntheticOutput()
        val planes = raw[0]
        planes[0][0] = 320f   // cx
        planes[1][0] = 320f   // cy
        planes[2][0] = 100f   // w
        planes[3][0] = 100f   // h
        planes[4][0] = 0.90f  // score

        val out = OutputDecoder.decode(raw, transform, confidenceThreshold = 0.5f)

        assertEquals(1, out.size)
        val box = out[0].box
        assertEquals(540f, box.left, 0.001f)    // (320-50 - 0) / 0.5
        assertEquals(260f, box.top, 0.001f)     // (320-50 - 140) / 0.5
        assertEquals(740f, box.right, 0.001f)
        assertEquals(460f, box.bottom, 0.001f)
        assertEquals(0.90f, out[0].score, 1e-6f)
        assertTrue(box.right > box.left && box.bottom > box.top)
    }

    @Test
    fun `filters out anchors below the confidence threshold`() {
        val raw = syntheticOutput()
        val planes = raw[0]
        planes[0][0] = 320f; planes[1][0] = 320f; planes[2][0] = 100f; planes[3][0] = 100f
        planes[4][0] = 0.90f
        planes[0][1] = 100f; planes[1][1] = 100f; planes[2][1] = 40f; planes[3][1] = 40f
        planes[4][1] = 0.40f   // below threshold

        val out = OutputDecoder.decode(raw, transform, confidenceThreshold = 0.5f)
        assertEquals(1, out.size)
        assertEquals(0.90f, out[0].score, 1e-6f)
    }

    @Test
    fun `anchor exactly at threshold is kept`() {
        val raw = syntheticOutput()
        val planes = raw[0]
        planes[4][0] = 0.75f
        planes[2][0] = 50f; planes[3][0] = 50f; planes[0][0] = 320f; planes[1][0] = 320f

        val out = OutputDecoder.decode(raw, transform, confidenceThreshold = 0.75f)
        assertEquals(1, out.size)
    }

    @Test
    fun `all-zero scores produce no detections at a real threshold`() {
        // Threshold is inclusive, so a 0.0 threshold would keep everything;
        // use the configured default to show zero-confidence anchors drop out.
        val out = OutputDecoder.decode(syntheticOutput(), transform, confidenceThreshold = 0.5f)
        assertTrue(out.isEmpty())
    }

    @Test
    fun `too few channel rows yields nothing`() {
        val raw = Array(1) { Array(3) { FloatArray(8400) } }
        assertTrue(OutputDecoder.decode(raw, transform, 0.0f).isEmpty())
    }

    @Test
    fun `non-tensor payloads yield nothing instead of crashing`() {
        assertTrue(OutputDecoder.decode(null, transform, 0.5f).isEmpty())
        assertTrue(OutputDecoder.decode("not a tensor", transform, 0.5f).isEmpty())
        assertTrue(OutputDecoder.decode(FloatArray(64), transform, 0.5f).isEmpty())
    }

    @Test
    fun `all anchors above threshold are all reported`() {
        val raw = syntheticOutput()
        val planes = raw[0]
        for (i in 0 until 8400) {
            planes[0][i] = 320f
            planes[1][i] = 320f
            planes[2][i] = 60f
            planes[3][i] = 60f
            planes[4][i] = 0.8f
        }
        val out = OutputDecoder.decode(raw, transform, 0.5f)
        assertEquals(8400, out.size)
        // ...which is exactly why NMS must run next.
        val kept = Nms.suppress(out, 0.45f)
        assertEquals(1, kept.size)
    }
}
