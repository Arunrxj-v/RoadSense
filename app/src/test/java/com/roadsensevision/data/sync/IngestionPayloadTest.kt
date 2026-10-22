package com.roadsensevision.data.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IngestionPayloadTest {

    @Test
    fun includesRequiredFieldsAndOptionalMetadata() {
        val json = IngestionPayload(
            reportId = "report-1",
            latitude = 8.893214,
            longitude = 76.614102,
            observedAt = "2026-10-21T09:32:11Z",
            confidence = 0.94f,
            gpsAccuracyM = 4.2f,
        ).toJson()

        assertTrue(json.contains("\"report_id\":\"report-1\""))
        assertTrue(json.contains("\"latitude\":8.893214"))
        assertTrue(json.contains("\"longitude\":76.614102"))
        assertTrue(json.contains("\"observed_at\":\"2026-10-21T09:32:11Z\""))
        assertTrue(json.contains("\"confidence\":0.94"))
        assertTrue(json.contains("\"gps_accuracy_m\":4.2"))
    }

    @Test
    fun omitsMissingOptionalMetadata() {
        val json = IngestionPayload(
            reportId = "report-2",
            latitude = 8.0,
            longitude = 76.0,
            observedAt = "2026-10-21T09:32:11Z",
            confidence = null,
            gpsAccuracyM = null,
        ).toJson()

        assertFalse(json.contains("confidence"))
        assertFalse(json.contains("gps_accuracy_m"))
    }
}
