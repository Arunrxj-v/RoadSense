package com.roadsensevision.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import com.roadsensevision.domain.models.FrameDetection
import com.roadsensevision.ui.theme.Primary
import kotlin.math.roundToInt

/**
 * Maps image (display-orientation) coordinates to preview-view coordinates
 * using a center-crop (FILL_CENTER) transform.
 */
data class ImageTransform(
    val scaleX: Float,
    val scaleY: Float,
    val offsetX: Float,
    val offsetY: Float
) {
    companion object {
        val Identity = ImageTransform(1f, 1f, 0f, 0f)

        /** Center-crop transform: scale to fill, then center. */
        fun fillCenter(imageW: Int, imageH: Int, viewW: Float, viewH: Float): ImageTransform {
            if (imageW <= 0 || imageH <= 0 || viewW <= 0f || viewH <= 0f) return Identity
            val scale = maxOf(viewW / imageW, viewH / imageH)
            return ImageTransform(
                scaleX = scale,
                scaleY = scale,
                offsetX = (viewW - imageW * scale) / 2f,
                offsetY = (viewH - imageH * scale) / 2f
            )
        }
    }
}

/**
 * Draws actual model detections over the camera preview:
 * 2px signal-orange square-corner bounding boxes with a solid label tag.
 */
@Composable
fun DetectionOverlay(
    detections: List<FrameDetection>,
    transform: ImageTransform,
    modifier: Modifier = Modifier
) {
    if (detections.isEmpty()) return

    val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = 0xFFEA580C.toInt()
        strokeJoin = Paint.Join.MITER
        isAntiAlias = false
    }
    val tagPaint = Paint().apply { color = 0xFFEA580C.toInt() }
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 26f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    Canvas(modifier = modifier) {
        val density = this
        boxPaint.strokeWidth = 4f * (density.density) // ~2dp stroke

        for (detection in detections) {
            val l = detection.box.left * transform.scaleX + transform.offsetX
            val t = detection.box.top * transform.scaleY + transform.offsetY
            val r = detection.box.right * transform.scaleX + transform.offsetX
            val b = detection.box.bottom * transform.scaleY + transform.offsetY
            if (r <= l || b <= t) continue

            // Bounding box: 2px solid orange, zero corner radius.
            drawRect(
                color = Color(0xFFEA580C),
                topLeft = Offset(l, t),
                size = Size(r - l, b - t),
                style = Stroke(width = boxPaint.strokeWidth)
            )

            // Label tag anchored above the top-left corner.
            val label = "Pothole · ${(detection.confidence * 100).roundToInt()}%"
            val textW = textPaint.measureText(label)
            val tagW = textW + 16f
            val tagH = 34f
            val tagTop = t - tagH
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawRect(l, tagTop, l + tagW, t, tagPaint)
                canvas.nativeCanvas.drawText(label, l + 8f, tagTop + tagH * 0.68f, textPaint)
            }
        }
    }
}
