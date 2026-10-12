package com.roadsensevision.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Preprocessing geometry tests: the letterbox scale/padding and the
 * un-letterbox mapping are what keep overlay boxes aligned with the preview.
 */
class LetterboxTransformTest {

    @Test
    fun `landscape frame is scaled to width and padded vertically`() {
        val t = LetterboxTransform.compute(1920, 1080)
        assertEquals(640f, t.scale * 1920f, 1f)
        assertEquals(0f, t.padX, 0.001f)
        assertEquals(140f, t.padY, 0.001f)   // (640 - 360) / 2
    }

    @Test
    fun `portrait frame is scaled to height and padded horizontally`() {
        val t = LetterboxTransform.compute(1080, 1920)
        assertEquals(640f, t.scale * 1920f, 1f)
        assertEquals(140f, t.padX, 0.001f)
        assertEquals(0f, t.padY, 0.001f)
    }

    @Test
    fun `square frame needs no padding and scale is one`() {
        val t = LetterboxTransform.compute(640, 640)
        assertEquals(1f, t.scale, 0.0001f)
        assertEquals(0f, t.padX, 0.001f)
        assertEquals(0f, t.padY, 0.001f)
    }

    @Test
    fun `fitted dimension never exceeds the model input size`() {
        for ((w, h) in listOf(1920 to 1080, 1080 to 1920, 3000 to 1000, 640 to 480, 1 to 1)) {
            val t = LetterboxTransform.compute(w, h)
            val newW = w * t.scale
            val newH = h * t.scale
            assertTrue("width overflow for ${w}x$h", newW <= 640.001f)
            assertTrue("height overflow for ${w}x$h", newH <= 640.001f)
            assertTrue(t.padX >= 0f && t.padY >= 0f)
        }
    }

    @Test
    fun `toSource and toModel are inverse of each other`() {
        val t = LetterboxTransform.compute(1920, 1080)
        val (mx, my) = t.toModel(960f, 540f)
        val back = t.toSource(
            com.roadsensevision.domain.models.BoxF(mx, my, mx + 10f, my + 10f)
        )
        assertEquals(960f, back.left, 0.01f)
        assertEquals(540f, back.top, 0.01f)
    }

    @Test
    fun `toSource removes padding then undoes scale`() {
        // Hand-built transform so the arithmetic is exact.
        val t = LetterboxTransform(size = 640, scale = 0.5f, padX = 0f, padY = 140f)
        val source = t.toSource(
            com.roadsensevision.domain.models.BoxF(270f, 270f, 370f, 370f)
        )
        assertEquals(540f, source.left, 0.001f)    // (270 - 0) / 0.5
        assertEquals(260f, source.top, 0.001f)     // (270 - 140) / 0.5
        assertEquals(740f, source.right, 0.001f)
        assertEquals(460f, source.bottom, 0.001f)
    }
}
