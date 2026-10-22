package com.roadsensevision.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IngestionClientResponseTest {

    private val client = IngestionClient(baseUrl = "", apiKey = "")

    @Test
    fun createdResponseIsSuccess() {
        val result = client.interpretResponse(
            status = 201,
            responseBody = """{"created":true,"duplicate_report":false,"pothole_id":"PH-000002","report_count":1}""",
        )

        assertTrue(result is IngestionResult.Success)
        result as IngestionResult.Success
        assertEquals("PH-000002", result.potholeId)
        assertFalse(result.duplicate)
    }

    @Test
    fun existingCanonicalResponseIsSuccess() {
        val result = client.interpretResponse(
            status = 200,
            responseBody = """{"created":false,"duplicate_report":false,"pothole_id":"PH-000001","report_count":4}""",
        )

        assertTrue(result is IngestionResult.Success)
        assertFalse((result as IngestionResult.Success).duplicate)
    }

    @Test
    fun duplicateResponseIsSuccessWithSameCanonicalId() {
        val result = client.interpretResponse(
            status = 200,
            responseBody = """{"created":false,"duplicate_report":true,"pothole_id":"PH-000002"}""",
        )

        assertTrue(result is IngestionResult.Success)
        result as IngestionResult.Success
        assertEquals("PH-000002", result.potholeId)
        assertTrue(result.duplicate)
    }

    @Test
    fun successWithoutPotholeIdIsRetryable() {
        val result = client.interpretResponse(200, """{"created":true}""")

        assertTrue(result is IngestionResult.Retryable)
        assertEquals(200, (result as IngestionResult.Retryable).statusCode)
    }

    @Test
    fun authorizationFailureIsPermanent() {
        val result = client.interpretResponse(401, """{"error":{"status":401}}""")

        assertTrue(result is IngestionResult.PermanentFailure)
        assertEquals(401, (result as IngestionResult.PermanentFailure).statusCode)
    }

    @Test
    fun validationFailureIsPermanent() {
        val result = client.interpretResponse(400, """{"error":{"status":400}}""")

        assertTrue(result is IngestionResult.PermanentFailure)
        assertEquals(400, (result as IngestionResult.PermanentFailure).statusCode)
    }

    @Test
    fun timeoutAndServerFailuresAreRetryable() {
        assertTrue(client.interpretResponse(408, "") is IngestionResult.Retryable)
        assertTrue(client.interpretResponse(429, "") is IngestionResult.Retryable)
        assertTrue(client.interpretResponse(500, "") is IngestionResult.Retryable)
        assertTrue(client.interpretResponse(503, "") is IngestionResult.Retryable)
    }
}
