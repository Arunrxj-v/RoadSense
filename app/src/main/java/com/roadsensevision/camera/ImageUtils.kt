package com.roadsensevision.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

/**
 * Converts an [ImageProxy] (RGBA_8888 output) to an upright bitmap
 * oriented to display, applying the frame's rotation.
 *
 * Handles row-stride padding by creating a padded bitmap whose row
 * bytes match the image stride, then cropping to the true width.
 */
fun ImageProxy.toUprightBitmap(): Bitmap? {
    return try {
        val planes = this.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val width = this.width
        val height = this.height
        if (width <= 0 || height <= 0 || pixelStride == 0) return null

        buffer.rewind()
        val paddedWidth = rowStride / pixelStride
        val full = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
        full.copyPixelsFromBuffer(buffer)

        val cropped = if (paddedWidth != width) {
            val c = Bitmap.createBitmap(full, 0, 0, width, height)
            full.recycle()
            c
        } else full

        val rotation = this.imageInfo.rotationDegrees
        if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            val rotated = Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
            if (rotated != cropped) cropped.recycle()
            rotated
        } else cropped
    } catch (e: Exception) {
        null
    }
}
