package com.roadsensevision.domain.models

import org.junit.Assert.assertEquals
import org.junit.Test

/** Severity is derived from confidence — thresholds are a documented contract. */
class SeverityTest {

    @Test
    fun `high confidence is severe`() {
        assertEquals(Severity.SEVERE, Severity.fromConfidence(0.95f))
        assertEquals(Severity.SEVERE, Severity.fromConfidence(0.85f))
    }

    @Test
    fun `mid confidence is moderate`() {
        assertEquals(Severity.MODERATE, Severity.fromConfidence(0.8499f))
        assertEquals(Severity.MODERATE, Severity.fromConfidence(0.70f))
    }

    @Test
    fun `low confidence is low`() {
        assertEquals(Severity.LOW, Severity.fromConfidence(0.6999f))
        assertEquals(Severity.LOW, Severity.fromConfidence(0.50f))
    }

    @Test
    fun `labels are human readable`() {
        assertEquals("Low", Severity.LOW.label)
        assertEquals("Moderate", Severity.MODERATE.label)
        assertEquals("Severe", Severity.SEVERE.label)
    }

    @Test
    fun `session and sync status wire values are stable for the database`() {
        assertEquals("ACTIVE", SessionStatus.ACTIVE.value)
        assertEquals("COMPLETED", SessionStatus.COMPLETED.value)
        assertEquals("PENDING", SyncStatus.PENDING.value)
    }

    @Test
    fun `box dimensions never go negative`() {
        val b = BoxF(100f, 100f, 50f, 40f)
        assertEquals(0f, b.width, 0f)
        assertEquals(0f, b.height, 0f)
    }
}
