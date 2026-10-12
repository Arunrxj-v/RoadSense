package com.roadsensevision.inference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.roadsensevision.domain.models.FrameDetection
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer

/**
 * Result of running the model on one frame.
 * [latencyMs] is the measured end-to-end inference time for this frame.
 */
data class DetectionResult(
    val detections: List<FrameDetection>,
    val latencyMs: Float,
)

/**
 * On-device pothole detector using ONNX Runtime (CPU execution provider).
 *
 * Pipeline (verified against the packaged `best.onnx`):
 *  - preprocess: letterbox the frame to 640x640 (pad 114), RGB, /255, NCHW;
 *  - inference:  one reused [OrtSession], float32 [1,3,640,640] -> [1,5,8400];
 *  - decode:     channels are cx, cy, w, h, score (single class, no NMS in graph);
 *  - post:       greedy NMS client-side ([Nms.suppress]), then un-letterbox
 *                boxes back to source image coordinates ([LetterboxTransform]).
 *
 * The session is created once in [initialize] and reused for every frame.
 * [detect] must be called off the main thread (the caller owns dispatching).
 */
class PotholeDetector(private val context: Context) {

    private var environment: OrtEnvironment? = null
    private var session: OrtSession? = null

    @Volatile
    var isReady: Boolean = false
        private set

    @Volatile
    var initializationError: String? = null
        private set

    /**
     * Loads the packaged model into a reusable session.
     * Safe to call from a background dispatcher; never call per-frame.
     *
     * @param threads intra-op thread count for the CPU EP
     * @return true when the session is ready for [detect]
     */
    fun initialize(threads: Int): Boolean {
        if (isReady && session != null) return true
        return try {
            val modelFile = ensureModelFile()
            val env = OrtEnvironment.getEnvironment()
            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(threads.coerceIn(1, 8))
            }
            val created = env.createSession(modelFile.absolutePath, options)
            environment = env
            session = created
            initializationError = null
            isReady = true
            true
        } catch (t: Throwable) {
            initializationError = t.message ?: t.javaClass.simpleName
            isReady = false
            android.util.Log.e(
                "RoadSenseV",
                "initialize: FAILED for ${ModelInfo.MODEL_FILE} (threads=${threads.coerceIn(1, 8)})",
                t
            )
            false
        }
    }

    /** Copies the packaged model from assets to app-private storage (once). */
    private fun ensureModelFile(): File {
        val dir = File(context.filesDir, "models").apply { mkdirs() }
        val out = File(dir, ModelInfo.MODEL_FILE)
        if (!out.exists() || out.length() == 0L) {
            context.assets.open("models/${ModelInfo.MODEL_FILE}").use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            }
        }
        return out
    }

    /**
     * Runs detection on a single frame bitmap (display orientation).
     * Caller must be off the main thread.
     */
    fun detect(
        bitmap: Bitmap,
        confidenceThreshold: Float,
        iouThreshold: Float
    ): DetectionResult {
        val activeSession = session
        if (!isReady || activeSession == null) {
            android.util.Log.w("RoadSenseV", "detect: not ready isReady=$isReady, session==null=${activeSession == null}")
            return DetectionResult(emptyList(), 0f)
        }
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            android.util.Log.w("RoadSenseV", "detect: invalid bitmap size ${bitmap.width}x${bitmap.height}")
            return DetectionResult(emptyList(), 0f)
        }

        val startNs = System.nanoTime()

        // 1. Letterbox geometry + NCHW float buffer.
        val transform = LetterboxTransform.compute(bitmap.width, bitmap.height)
        android.util.Log.i("RoadSenseV", "detect: letterbox transform scale=${transform.scale} padX=${transform.padX} padY=${transform.padY} newW=${transform.size}x${transform.size}")

        val input = letterboxToInput(bitmap, transform)

        android.util.Log.i("RoadSenseV", "detect: input buffer size=${input.size}, first4=${input.take(4).joinToString()}")

        // 2. Inference (one reused session; close inputs/outputs deterministically).
        val rawOutput: Any? = try {
            val env = environment ?: OrtEnvironment.getEnvironment()
            val shape = longArrayOf(
                1L, 3L, ModelInfo.INPUT_DIM.toLong(), ModelInfo.INPUT_DIM.toLong()
            )
            val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(input), shape)
            try {
                val feeds = mapOf<String, OnnxTensor>(ModelInfo.INPUT_NAME to tensor)
                android.util.Log.i("RoadSenseV", "detect: running inference with session, input=${ModelInfo.INPUT_NAME}")
                val result = activeSession.run(feeds)
                val raw = result[0].value
                android.util.Log.i(
                    "RoadSenseV",
                    "detect: raw output shape=${describeShape(raw)}, valueClass=${raw?.javaClass?.name ?: "null"}"
                )
                logScoreChannelStats(raw)
                result.close()
                raw
            } finally {
                tensor.close()
            }
        } catch (t: Throwable) {
            val latency = (System.nanoTime() - startNs) / 1_000_000f
            android.util.Log.e("RoadSenseV", "detect: INFEERENCE EXCEPTION", t)
            return DetectionResult(emptyList(), latency)
        }

        android.util.Log.i("RoadSenseV", "detect: post-inference, about to decode with threshold=$confidenceThreshold")

        // 3. Decode [1,5,8400] -> candidate boxes, un-letterboxed to source pixels.
        val candidates = OutputDecoder.decode(rawOutput, transform, confidenceThreshold)
        android.util.Log.i("RoadSenseV", "detect: decode produced ${candidates.size} candidates at threshold=$confidenceThreshold")

        // 4. Client-side NMS (the graph has no NMS node).
        val kept = Nms.suppress(candidates, iouThreshold)
        android.util.Log.i("RoadSenseV", "detect: NMS kept ${kept.size} of ${candidates.size} candidates (iouThreshold=$iouThreshold)")

        val detections = kept.map {
            FrameDetection(box = it.box, confidence = it.score, captureTimeMs = 0L)
        }
        val latency = (System.nanoTime() - startNs) / 1_000_000f
        return DetectionResult(detections, latency)
    }

    /**
     * Derives a `[d0, d1, d2]` shape string from the nested output arrays.
     * ORT 1.18's `Result.Value` does not expose the tensor shape directly.
     */
    private fun describeShape(raw: Any?): String {
        val d0 = raw as? Array<*> ?: return "n/a"
        val d1 = d0.firstOrNull() as? Array<*> ?: return "[${d0.size}, ?]"
        val d2 = d1.firstOrNull() as? FloatArray ?: return "[${d0.size}, ${d1.size}, ?]"
        return "[${d0.size}, ${d1.size}, ${d2.size}]"
    }

    /**
     * Summarizes the raw `output0` score channel (index 4 of `[1,5,8400]`).
     * A near-zero maximum here means the model saw no pothole-like content in
     * the frame (or preprocessing is wrong); a healthy maximum below the UI
     * threshold means the threshold is the blocker.
     */
    private fun logScoreChannelStats(raw: Any?) {
        try {
            val tensor = raw as? Array<Array<FloatArray>>
            if (tensor == null) {
                android.util.Log.w(
                    "RoadSenseV",
                    "detect: raw output is ${raw?.javaClass?.name ?: "null"}, expected Array<Array<FloatArray>>"
                )
                return
            }
            val planes = tensor.firstOrNull()
            if (planes == null || planes.size < 5) {
                android.util.Log.w("RoadSenseV", "detect: channel count=${planes?.size ?: 0}, expected >= 5")
                return
            }
            val scores = planes[4]
            var min = Float.MAX_VALUE
            var max = -Float.MAX_VALUE
            var over25 = 0
            var over50 = 0
            for (s in scores) {
                if (s < min) min = s
                if (s > max) max = s
                if (s >= 0.25f) over25++
                if (s >= 0.50f) over50++
            }
            android.util.Log.i(
                "RoadSenseV",
                ("detect: score channel n=%d range [%.5f .. %.5f] >=0.25:%d >=0.50:%d")
                    .format(java.util.Locale.US, scores.size, min, max, over25, over50)
            )
        } catch (t: Throwable) {
            android.util.Log.w("RoadSenseV", "detect: score stats failed: ${t.message}")
        }
    }

    /**
     * Letterboxes the frame into a 640x640 canvas (pad 114) and packs the
     * pixels into a NCHW float32 buffer normalized to 0..1 (RGB order).
     */
    private fun letterboxToInput(src: Bitmap, transform: LetterboxTransform): FloatArray {
        val size = transform.size
        val newW = (src.width * transform.scale).toInt().coerceIn(1, size)
        val newH = (src.height * transform.scale).toInt().coerceIn(1, size)

        val scaled =
            if (newW == src.width && newH == src.height) src
            else Bitmap.createScaledBitmap(src, newW, newH, true)

        val canvasBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBmp)
        canvas.drawColor(Color.rgb(114, 114, 114))
        canvas.drawBitmap(scaled, transform.padX, transform.padY, Paint(Paint.FILTER_BITMAP_FLAG))
        if (scaled !== src) scaled.recycle()

        val pixels = IntArray(size * size)
        canvasBmp.getPixels(pixels, 0, size, 0, 0, size, size)
        canvasBmp.recycle()

        val plane = size * size
        val chw = FloatArray(3 * plane)
        for (i in pixels.indices) {
            val c = pixels[i]
            chw[i] = ((c shr 16) and 0xFF) / 255f                 // R
            chw[plane + i] = ((c shr 8) and 0xFF) / 255f          // G
            chw[2 * plane + i] = (c and 0xFF) / 255f              // B
        }
        return chw
    }

    /** Releases the session. The process-wide OrtEnvironment singleton is left intact. */
    fun release() {
        try {
            session?.close()
        } catch (_: Throwable) {
        }
        session = null
        environment = null
        isReady = false
    }
}
