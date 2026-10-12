package com.roadsensevision.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.roadsensevision.ui.theme.Primary
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** A single map marker (a recorded pothole location). */
data class MarkerData(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val snippet: String
)

/** Builds a signal-orange circular pin bitmap with an optional label. */
fun buildPinBitmap(context: Context, label: String? = null, selected: Boolean = false): Bitmap {
    val size = if (selected) 96 else 80
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f

    // Outer white ring
    val ringPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, size / 2f - 4f, ringPaint)

    // Orange core
    val corePaint = Paint().apply {
        color = Primary.toArgb()
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, size / 2f - 10f, corePaint)

    if (label != null) {
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = size * 0.34f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val baseline = cy - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(label, cx, baseline, textPaint)
    }
    return bitmap
}

/**
 * osmdroid map view rendering only locally recorded defect markers.
 * Tiles (OpenStreetMap) require connectivity; markers and controls
 * work regardless and reflect stored coordinates.
 */
@Composable
fun OsmdroidMap(
    markers: List<MarkerData>,
    modifier: Modifier = Modifier,
    initialZoom: Double = 16.0,
    selectedId: String? = null,
    onMarkerClick: (MarkerData) -> Unit,
    recenterToken: Int = 0
) {
    AndroidView(
        factory = { context ->
            Configuration.getInstance().userAgentValue = context.packageName
            val mapView = MapView(context)
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.setMultiTouchControls(true)
            mapView.controller.setZoom(initialZoom)
            mapView
        },
        update = { mapView ->
            mapView.overlays.clear()
            for (m in markers) {
                if (m.latitude.isNaN() || m.longitude.isNaN()) continue
                val marker = Marker(mapView)
                marker.position = GeoPoint(m.latitude, m.longitude)
                marker.title = m.title
                marker.snippet = m.snippet
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                marker.icon = android.graphics.drawable.BitmapDrawable(
                    mapView.resources,
                    buildPinBitmap(mapView.context, m.id.takeLast(2), m.id == selectedId)
                )
                marker.setOnMarkerClickListener { _, _ ->
                    onMarkerClick(m)
                    true
                }
                mapView.overlays.add(marker)
            }
            // Center on the selected marker (or first) when recenter is requested.
            val target = markers.firstOrNull { it.id == selectedId } ?: markers.firstOrNull()
            if (target != null && recenterToken >= 0) {
                mapView.controller.setZoom(if (selectedId != null) 17.0 else initialZoom)
                mapView.controller.setCenter(GeoPoint(target.latitude, target.longitude))
            }
            mapView.invalidate()
        },
        modifier = modifier
    )
}

/** Format a Compose color to android int (utility). */
fun ComposeColor.toAndroidInt(): Int = toArgb()
