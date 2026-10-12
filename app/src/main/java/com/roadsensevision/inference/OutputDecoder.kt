package com.roadsensevision.inference

import com.roadsensevision.domain.models.BoxF

/**
 * Pure decoder for the raw `output0` tensor of the packaged model.
 *
 * Layout is `[1, 5, 8400]` — rows are `cx, cy, w, h, score` expressed in
 * model (640) space for a single class (`pothole`). The graph contains no
 * NMS node, so [Nms.suppress] must be applied afterwards.
 *
 * Kept free of Android/ORT types so decoding and un-letterbox mapping are
 * unit-testable on the JVM against a synthetic (or real) output tensor.
 * Frame diagnostics (score-channel range, candidate counts) are logged by
 * [PotholeDetector], which owns the Android side of the pipeline.
 */
object OutputDecoder {

    /**
     * @param rawOutput        the value of `output0`, i.e. `Array<Array<FloatArray>>`
     * @param transform        letterbox geometry used for this frame
     * @param confidenceThreshold minimum score to keep (inclusive)
     * @return candidate boxes already mapped into source-image pixels
     */
    @Suppress("UNCHECKED_CAST")
    fun decode(
        rawOutput: Any?,
        transform: LetterboxTransform,
        confidenceThreshold: Float,
    ): List<ScoredBox> {
        val tensor = rawOutput as? Array<Array<FloatArray>> ?: return emptyList()
        val planes = tensor.firstOrNull() ?: return emptyList()
        if (planes.size < 5) return emptyList()

        val cx = planes[0]
        val cy = planes[1]
        val w = planes[2]
        val h = planes[3]
        val score = planes[4]
        val count = score.size

        val out = ArrayList<ScoredBox>(16)
        for (i in 0 until count) {
            val s = score[i]
            if (s < confidenceThreshold) continue

            val halfW = w[i] / 2f
            val halfH = h[i] / 2f
            val modelBox = BoxF(
                left = cx[i] - halfW,
                top = cy[i] - halfH,
                right = cx[i] + halfW,
                bottom = cy[i] + halfH,
            )
            out.add(ScoredBox(box = transform.toSource(modelBox), score = s))
        }
        return out
    }
}
