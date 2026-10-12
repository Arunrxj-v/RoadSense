package com.roadsensevision.domain.detection

/**
 * Duplicate-prevention policy: decides whether a newly confirmed detection
 * should be attached to an existing road defect or start a new one.
 *
 * Rules (Phase 1):
 *  - **Space**: the distance must be within `mergeRadiusMeters` plus an
 *    accuracy allowance of `2 * (accuracyA + accuracyB)`, because a GPS fix
 *    with poor accuracy should not be trusted to separate two defects.
 *  - **Time**: the observation must fall within the `mergeWindowMs` window.
 *    GPS proximity alone is never treated as definitive proof of identity —
 *    a pothole seen on a different day at the same coordinates is a separate
 *    record only if the window has elapsed.
 *
 * Pure Kotlin — unit-testable without Android.
 */
object DuplicatePolicy {

    fun shouldMerge(
        distanceMeters: Double,
        mergeRadiusMeters: Float,
        accuracyA: Float?,
        accuracyB: Float?,
        timeDeltaMs: Long,
        mergeWindowMs: Long,
    ): Boolean {
        val accuracyAllowance = 2f * ((accuracyA ?: 0f) + (accuracyB ?: 0f))
        val withinSpace = distanceMeters <= mergeRadiusMeters + accuracyAllowance
        val withinTime = timeDeltaMs in 0L..mergeWindowMs
        return withinSpace && withinTime
    }
}
