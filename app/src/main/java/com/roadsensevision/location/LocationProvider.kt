package com.roadsensevision.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.roadsensevision.domain.models.LocationQuality
import java.util.concurrent.ConcurrentLinkedDeque

/** GPS sample associated with a detection. */
data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMs: Long
)

enum class GpsState {
    DISABLED,      // permission denied or provider off
    SEARCHING,     // listening, no fix yet
    ACTIVE,        // receiving fixes
    UNAVAILABLE
}

/**
 * Wraps Google Play Services FusedLocationProvider.
 * Keeps a short rolling cache of samples so observations can be
 * associated with the GPS fix closest in time to the detection.
 */
class LocationProvider(private val appContext: Context) {

    private val fused = LocationServices.getFusedLocationProviderClient(appContext)
    private val samples = ConcurrentLinkedDeque<LocationSample>()
    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (location in result.locations) {
                addSample(location)
            }
        }
    }

    @Volatile var state: GpsState = GpsState.DISABLED
        private set

    val lastSample: LocationSample?
        get() = samples.peekLast()

    @SuppressLint("MissingPermission")
    fun start() {
        state = GpsState.SEARCHING
        try {
            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000L
            ).apply {
                setMinUpdateIntervalMillis(500L)
                setMaxUpdateDelayMillis(2000L)
            }.build()
            fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            state = GpsState.DISABLED
        } catch (e: Exception) {
            state = GpsState.UNAVAILABLE
        }
    }

    fun stop() {
        try {
            fused.removeLocationUpdates(callback)
        } catch (_: Exception) {}
    }

    private fun addSample(location: Location) {
        state = GpsState.ACTIVE
        samples.addLast(
            LocationSample(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracy,
                timestampMs = location.time
            )
        )
        // Keep a rolling window (last ~60s of samples at ~1-2 Hz).
        while (samples.size > 90) samples.removeFirst()
    }

    /**
     * Pick the GPS sample closest in time to [targetTimeMs].
     * Returns null when no fix exists within [maxAgeMs].
     */
    fun sampleNear(targetTimeMs: Long, maxAgeMs: Long = 2500L): LocationSample? {
        var best: LocationSample? = null
        var bestDelta = Long.MAX_VALUE
        for (sample in samples) {
            val delta = Math.abs(sample.timestampMs - targetTimeMs)
            if (delta < bestDelta) {
                bestDelta = delta
                best = sample
            }
        }
        return if (best != null && bestDelta <= maxAgeMs) best else null
    }

    fun qualityOf(sample: LocationSample?): LocationQuality = when {
        sample == null -> LocationQuality.UNAVAILABLE
        sample.accuracyMeters <= 5f -> LocationQuality.GOOD
        sample.accuracyMeters <= 15f -> LocationQuality.FAIR
        else -> LocationQuality.POOR
    }

    fun clear() {
        samples.clear()
    }
}
