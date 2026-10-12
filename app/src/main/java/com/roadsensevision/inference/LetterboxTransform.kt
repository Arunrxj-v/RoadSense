package com.roadsensevision.inference

import com.roadsensevision.domain.models.BoxF
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Letterbox transform for the model's square input.
 *
 * The source frame is scaled to fit [size]x[size] while preserving aspect
 * ratio, then centered with the remainder padded (fill 114). Decoding happens
 * in model space; [toSource] maps boxes back into source-image pixels so the
 * overlay aligns with the camera preview.
 *
 * Pure arithmetic — unit-testable without the Android framework.
 */
data class LetterboxTransform(
    val size: Int,
    val scale: Float,
    val padX: Float,
    val padY: Float,
) {
    /** Maps a box from model (640) space back to source-image pixels. */
    fun toSource(box: BoxF): BoxF = BoxF(
        left = (box.left - padX) / scale,
        top = (box.top - padY) / scale,
        right = (box.right - padX) / scale,
        bottom = (box.bottom - padY) / scale,
    )

    /** Maps a source-image coordinate into model (640) space. */
    fun toModel(sourceX: Float, sourceY: Float): Pair<Float, Float> =
        (sourceX * scale + padX) to (sourceY * scale + padY)

    companion object {
        /** Computes the scale and centering offsets for a given source size. */
        fun compute(
            srcWidth: Int,
            srcHeight: Int,
            size: Int = ModelInfo.INPUT_DIM,
        ): LetterboxTransform {
            require(srcWidth > 0 && srcHeight > 0) { "source size must be positive" }
            val scale = min(size.toFloat() / srcWidth, size.toFloat() / srcHeight)
            val newW = (srcWidth * scale).roundToInt().coerceIn(1, size)
            val newH = (srcHeight * scale).roundToInt().coerceIn(1, size)
            return LetterboxTransform(
                size = size,
                scale = scale,
                padX = (size - newW) / 2f,
                padY = (size - newH) / 2f,
            )
        }
    }
}
