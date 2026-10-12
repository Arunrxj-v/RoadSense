package com.roadsensevision.inference

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.FloatBuffer

/**
 * Loads the packaged [ModelInfo.MODEL_FILE] with the JVM ONNX Runtime and
 * verifies the graph contract the app depends on: names, shapes, layout, and
 * that decode + un-letterbox + NMS run end-to-end on a real output tensor.
 *
 * This mirrors the on-device pipeline without needing an Android device —
 * it proves the preprocessing/decode assumptions in [ModelInfo] are correct.
 */
class OnnxModelContractTest {

    @Test
    fun `model input and output names match the app contract`() {
        requireModel()
        assertEquals("images", ModelInfo.INPUT_NAME)
        assertEquals("output0", ModelInfo.OUTPUT_NAME)
        assertTrue(ModelInfo.INPUT_NAME in session.inputNames)
        assertTrue(ModelInfo.OUTPUT_NAME in session.outputNames)
    }

    @Test
    fun `graph produces the expected 1x5x8400 output for a 1x3x640x640 input`() {
        requireModel()
        val output = runSyntheticFrame()

        // [1, 5, 8400] -> outer batch dim 1, 5 channel rows, 8400 anchors.
        assertEquals(1, output.size)
        assertEquals(5, output[0].size)
        assertEquals(ModelInfo.NUM_ANCHORS, output[0][0].size)
        assertEquals(ModelInfo.NUM_ANCHORS, output[0][4].size)

        // Every value must be finite (no NaN/Inf leaking into the decoder).
        for (row in output[0]) {
            for (v in row) {
                assertTrue("non-finite output value", v.isFinite())
            }
        }
    }

    @Test
    fun `single-class decode plus NMS runs cleanly on a real frame`() {
        requireModel()
        val output = runSyntheticFrame()
        val transform = LetterboxTransform.compute(1920, 1080)

        // Permissive threshold: exercise as much of the decode path as possible.
        val candidates = OutputDecoder.decode(output, transform, confidenceThreshold = 0.01f)
        for (c in candidates) {
            assertTrue(c.box.right.isFinite() && c.box.left.isFinite())
            assertTrue(c.box.bottom.isFinite() && c.box.top.isFinite())
            assertTrue(c.score >= 0.01f)
        }

        val kept = Nms.suppress(candidates, iouThreshold = 0.45f)
        assertTrue("NMS must not grow the candidate set", kept.size <= candidates.size)
        // Scores must come back in descending order.
        kept.zipWithNext().forEach { (a, b) -> assertTrue(a.score >= b.score) }

        // A strict threshold on the same tensor must return nothing extra.
        val strict = OutputDecoder.decode(output, transform, confidenceThreshold = 0.999f)
        assertTrue(strict.all { it.score >= 0.999f })
    }

    @Test
    fun `letterboxed round trip keeps decoded boxes inside the source frame scale`() {
        requireModel()
        val output = runSyntheticFrame()
        val transform = LetterboxTransform.compute(1920, 1080)
        val candidates = OutputDecoder.decode(output, transform, confidenceThreshold = 0.01f)
        for (c in candidates) {
            // Source-image coordinates should land near the 1920x1080 frame
            // (allow generous slack for off-frame predictions).
            assertTrue(c.box.left > -2000f && c.box.left < 4000f)
            assertTrue(c.box.top > -2000f && c.box.top < 4000f)
            assertTrue(c.box.right > c.box.left)
            assertTrue(c.box.bottom > c.box.top)
        }
    }

    // ---- helpers ------------------------------------------------------------

    private fun runSyntheticFrame(): Array<Array<FloatArray>> {
        val size = ModelInfo.INPUT_DIM
        val chw = FloatArray(3 * size * size)
        // Light background with a darker central patch — structural contrast.
        for (c in 0 until 3) {
            val plane = c * size * size
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val dark = x in 220..420 && y in 260..460
                    chw[plane + y * size + x] = if (dark) 0.15f else 0.70f
                }
            }
        }
        val shape = longArrayOf(1L, 3L, size.toLong(), size.toLong())
        val env = OrtEnvironment.getEnvironment()
        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(chw), shape)
        try {
            val result = session.run(mapOf(ModelInfo.INPUT_NAME to tensor))
            try {
                @Suppress("UNCHECKED_CAST")
                return result[0].value as Array<Array<FloatArray>>
            } finally {
                result.close()
            }
        } finally {
            tensor.close()
        }
    }

    companion object {
        private lateinit var session: OrtSession
        private var modelFound = false

        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            val path = locateModel() ?: return
            modelFound = true
            val env = OrtEnvironment.getEnvironment()
            val opts = OrtSession.SessionOptions().apply { setIntraOpNumThreads(2) }
            session = env.createSession(path, opts)
        }

        private fun locateModel(): String? {
            val candidates = listOf(
                "src/main/assets/models/${ModelInfo.MODEL_FILE}",
                "../src/main/assets/models/${ModelInfo.MODEL_FILE}",
                "app/src/main/assets/models/${ModelInfo.MODEL_FILE}",
                "../Model/${ModelInfo.MODEL_FILE}",
                "Model/${ModelInfo.MODEL_FILE}",
            )
            return candidates.firstOrNull { File(it).exists() }
        }
    }

    private fun requireModel() {
        assumeTrue(
            "best.onnx not found relative to the test working directory — skipping",
            modelFound
        )
    }
}
