package com.roadsensevision.inference

import com.roadsensevision.domain.models.BoxF

/** A candidate detection box with its confidence score (pure Kotlin). */
data class ScoredBox(val box: BoxF, val score: Float)

/**
 * Greedy non-maximum suppression.
 *
 * The packaged `best.onnx` graph has **no** NMS node (verified: output is a raw
 * `[1, 5, 8400]` tensor), so overlap resolution is performed client-side here.
 */
object Nms {

    /** Intersection-over-union of two axis-aligned boxes, in `[0, 1]`. */
    fun iou(a: BoxF, b: BoxF): Float {
        val interLeft = maxOf(a.left, b.left)
        val interRight = minOf(a.right, b.right)
        val interTop = maxOf(a.top, b.top)
        val interBottom = minOf(a.bottom, b.bottom)
        val interArea = maxOf(0f, interRight - interLeft) * maxOf(0f, interBottom - interTop)
        if (interArea <= 0f) return 0f
        val areaA = maxOf(0f, a.right - a.left) * maxOf(0f, a.bottom - a.top)
        val areaB = maxOf(0f, b.right - b.left) * maxOf(0f, b.bottom - b.top)
        val union = areaA + areaB - interArea
        return if (union <= 0f) 0f else interArea / union
    }

    /**
     * Keeps the highest-scoring box and suppresses every later box that
     * overlaps it by more than [iouThreshold], repeating greedily.
     *
     * @return survivors sorted by descending score
     */
    fun suppress(candidates: List<ScoredBox>, iouThreshold: Float): List<ScoredBox> {
        if (candidates.size <= 1) return candidates
        val sorted = candidates.sortedByDescending { it.score }
        val suppressed = BooleanArray(sorted.size)
        val kept = ArrayList<ScoredBox>(sorted.size)
        for (i in sorted.indices) {
            if (suppressed[i]) continue
            val winner = sorted[i]
            kept.add(winner)
            for (j in i + 1 until sorted.size) {
                if (suppressed[j]) continue
                if (iou(winner.box, sorted[j].box) > iouThreshold) suppressed[j] = true
            }
        }
        return kept
    }
}
