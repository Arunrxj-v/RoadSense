package com.roadsensevision.domain.detection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Duplicate-prevention tests: GPS proximity alone must never merge two
 * defects — the time window has to hold as well — and poor accuracy widens
 * the merge radius instead of splitting a pothole into many records.
 */
class DuplicatePolicyTest {

    private val radius = 25f
    private val window = 30 * 60_000L   // 30 minutes

    @Test
    fun `nearby and recent observation merges`() {
        assertTrue(
            DuplicatePolicy.shouldMerge(
                distanceMeters = 8.0,
                mergeRadiusMeters = radius,
                accuracyA = 4f,
                accuracyB = 4f,
                timeDeltaMs = 5_000L,
                mergeWindowMs = window,
            )
        )
    }

    @Test
    fun `beyond the radius does not merge`() {
        assertFalse(
            DuplicatePolicy.shouldMerge(
                distanceMeters = 60.0,
                mergeRadiusMeters = radius,
                accuracyA = 3f,
                accuracyB = 3f,
                timeDeltaMs = 5_000L,
                mergeWindowMs = window,
            )
        )
    }

    @Test
    fun `accuracy allowance can bridge a moderate gap`() {
        // 40 m apart, radius 25 m, but 2*(5+5) = 20 m of allowance -> 45 m budget.
        assertTrue(
            DuplicatePolicy.shouldMerge(
                distanceMeters = 40.0,
                mergeRadiusMeters = radius,
                accuracyA = 5f,
                accuracyB = 5f,
                timeDeltaMs = 1_000L,
                mergeWindowMs = window,
            )
        )
    }

    @Test
    fun `poor accuracy still has a limit`() {
        // 40 m apart, radius 25 m, 2*(50+50) = 200 m allowance -> 225 m budget
        // would merge; make the distance exceed even that generous budget.
        assertFalse(
            DuplicatePolicy.shouldMerge(
                distanceMeters = 250.0,
                mergeRadiusMeters = radius,
                accuracyA = 50f,
                accuracyB = 50f,
                timeDeltaMs = 1_000L,
                mergeWindowMs = window,
            )
        )
    }

    @Test
    fun `same location on a different day does not merge`() {
        assertFalse(
            DuplicatePolicy.shouldMerge(
                distanceMeters = 3.0,
                mergeRadiusMeters = radius,
                accuracyA = 5f,
                accuracyB = 5f,
                timeDeltaMs = window + 1,
                mergeWindowMs = window,
            )
        )
    }

    @Test
    fun `observation older than the existing defect does not merge`() {
        assertFalse(
            DuplicatePolicy.shouldMerge(
                distanceMeters = 3.0,
                mergeRadiusMeters = radius,
                accuracyA = 5f,
                accuracyB = 5f,
                timeDeltaMs = -1L,
                mergeWindowMs = window,
            )
        )
    }

    @Test
    fun `exact boundary of both gates still merges`() {
        assertTrue(
            DuplicatePolicy.shouldMerge(
                distanceMeters = radius.toDouble(),
                mergeRadiusMeters = radius,
                accuracyA = 0f,
                accuracyB = 0f,
                timeDeltaMs = window,
                mergeWindowMs = window,
            )
        )
    }

    @Test
    fun `missing accuracy is treated as zero rather than trusted`() {
        assertFalse(
            DuplicatePolicy.shouldMerge(
                distanceMeters = 30.0,
                mergeRadiusMeters = radius,
                accuracyA = null,
                accuracyB = null,
                timeDeltaMs = 1_000L,
                mergeWindowMs = window,
            )
        )
        // ...but with accurate fixes on both sides the same 30 m gap merges.
        assertTrue(
            DuplicatePolicy.shouldMerge(
                distanceMeters = 30.0,
                mergeRadiusMeters = radius,
                accuracyA = 4f,
                accuracyB = 4f,
                timeDeltaMs = 1_000L,
                mergeWindowMs = window,
            )
        )
    }
}
