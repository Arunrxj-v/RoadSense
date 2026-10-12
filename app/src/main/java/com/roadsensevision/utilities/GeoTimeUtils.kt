package com.roadsensevision.utilities

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sqrt

object GeoUtils {
    /** Haversine distance in metres between two WGS84 coordinates. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin2(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * sin2(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun sin2(x: Double): Double = sin(x) * sin(x)

    private fun sin(x: Double) = kotlin.math.sin(x)

    fun formatCoordinate(lat: Double?, lon: Double?): String {
        if (lat == null || lon == null) return "Unavailable"
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"
        return String.format(Locale.US, "%.5f° %s, %.5f° %s", abs(lat), latDir, abs(lon), lonDir)
    }

    fun formatAccuracy(accuracy: Float?): String =
        if (accuracy == null) "± —" else String.format(Locale.US, "±%.0f m", accuracy)
}

object TimeUtils {
    private val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("MMM d, hh:mm a", Locale.US)
    private val clockFormat = SimpleDateFormat("HH:mm", Locale.US)

    fun formatTime(ms: Long) = timeFormat.format(Date(ms))
    fun formatDateTime(ms: Long) = dateTimeFormat.format(Date(ms))
    fun formatClock(ms: Long) = clockFormat.format(Date(ms))

    fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }

    fun formatMinutes(ms: Long): Int = (ms / 60000).toInt()
}
