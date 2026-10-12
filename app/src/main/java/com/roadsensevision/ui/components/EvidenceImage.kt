package com.roadsensevision.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import com.roadsensevision.ui.theme.SurfaceHigh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads an evidence bitmap (relative path) off the main thread and renders it.
 * Falls back to a neutral surface block while loading or on failure.
 */
@Composable
fun EvidenceImage(
    relativePath: String?,
    loader: (String) -> Bitmap?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var bitmap by remember(relativePath) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(relativePath) {
        bitmap = if (relativePath == null) {
            null
        } else {
            withContext(Dispatchers.IO) { loader(relativePath) }
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Pothole evidence",
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(modifier.background(SurfaceHigh))
    }
}
