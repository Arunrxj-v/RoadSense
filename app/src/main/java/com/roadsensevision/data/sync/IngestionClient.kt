package com.roadsensevision.data.sync

import android.util.Log
import com.roadsensevision.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface IngestionResult {
    data class Success(val potholeId: String, val duplicate: Boolean) : IngestionResult
    data class Retryable(val message: String, val statusCode: Int? = null) : IngestionResult
    data class PermanentFailure(val message: String, val statusCode: Int? = null) : IngestionResult
}

class IngestionClient(
    private val baseUrl: String = BuildConfig.API_BASE_URL,
    private val apiKey: String = BuildConfig.INGEST_API_KEY,
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && apiKey.isNotBlank()

    private val apiBaseUrl: String
        get() {
            val normalized = baseUrl.trim().trimEnd('/')
            return if (normalized.endsWith("/api/v1")) normalized else "$normalized/api/v1"
        }

    suspend fun upload(payload: IngestionPayload): IngestionResult = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext IngestionResult.PermanentFailure("Ingestion API is not configured.")
        }

        var connection: HttpURLConnection? = null
        try {
            val endpoint = URL("$apiBaseUrl/internal/reports")
            Log.i(TAG, "UPLOAD START report=${payload.reportId} url=$endpoint")
            connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                doOutput = true
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }

            val body = payload.toJson().toByteArray(Charsets.UTF_8)
            Log.i(TAG, "PAYLOAD BUILT report=${payload.reportId} bytes=${body.size}")
            connection.setFixedLengthStreamingMode(body.size)
            Log.i(TAG, "STARTING HTTP REQUEST report=${payload.reportId}")
            connection.outputStream.use { it.write(body) }
            Log.i(TAG, "HTTP REQUEST SENT report=${payload.reportId}")

            val status = connection.responseCode
            Log.i(TAG, "HTTP RESPONSE RECEIVED report=${payload.reportId} status=$status")
            val responseBody = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            interpretResponse(status, responseBody)
        } catch (error: IOException) {
            Log.w(
                TAG,
                "UPLOAD FAILURE report=${payload.reportId} ${error.javaClass.name}: ${error.message}",
                error,
            )
            IngestionResult.Retryable(error.message ?: "Network request failed.")
        } catch (error: Exception) {
            Log.e(
                TAG,
                "UPLOAD FAILURE report=${payload.reportId} ${error.javaClass.name}: ${error.message}",
                error,
            )
            IngestionResult.PermanentFailure(error.message ?: "Ingestion request failed.")
        } finally {
            connection?.disconnect()
        }
    }

    internal fun interpretResponse(status: Int, responseBody: String): IngestionResult {
        val potholeId = POTHOLE_ID.find(responseBody)?.groupValues?.get(1)
        return when {
            status in 200..299 -> {
                if (potholeId == null) {
                    IngestionResult.Retryable("Ingestion response did not include a pothole id.", status)
                } else {
                    IngestionResult.Success(
                        potholeId = potholeId,
                        duplicate = responseBody.contains("\"duplicate_report\":true"),
                    )
                }
            }
            status == HttpURLConnection.HTTP_UNAUTHORIZED ||
                status == HttpURLConnection.HTTP_FORBIDDEN ||
                status == HttpURLConnection.HTTP_BAD_REQUEST ||
                status == HttpURLConnection.HTTP_NOT_FOUND ||
                status == HttpURLConnection.HTTP_ENTITY_TOO_LARGE ||
                status == 422 -> IngestionResult.PermanentFailure(
                "Ingestion rejected with HTTP $status.",
                status,
            )
            status == HttpURLConnection.HTTP_CLIENT_TIMEOUT || status == 429 || status >= 500 ->
                IngestionResult.Retryable("Ingestion temporarily failed with HTTP $status.", status)
            else -> IngestionResult.PermanentFailure(
                "Ingestion failed with HTTP $status.",
                status,
            )
        }
    }

    private companion object {
        const val TAG = "IngestionSync"
        val POTHOLE_ID = Regex("\"pothole_id\"\\s*:\\s*\"([^\"]+)\"")
    }
}
