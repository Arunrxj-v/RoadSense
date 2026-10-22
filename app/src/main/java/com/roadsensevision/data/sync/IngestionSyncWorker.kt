package com.roadsensevision.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.roadsensevision.data.repository.DetectionRepository
import java.time.Instant

class IngestionSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val client = IngestionClient()
        if (!client.isConfigured) return Result.success()

        val repository = DetectionRepository(applicationContext)
        var retryNeeded = false

        SyncScheduler.beginUpload()
        try {
            val observations = repository.getObservationsAwaitingSync(MAX_BATCH_SIZE)
            Log.i(TAG, "QUEUE ITEM FOUND count=${observations.size}")
            for (observation in observations) {
                val latitude = observation.latitude
                val longitude = observation.longitude
                if (latitude == null || longitude == null) {
                    Log.w(TAG, "QUEUE ITEM SKIPPED report=${observation.observationId} missing coordinates")
                    repository.markSyncFailed(observation.observationId)
                    continue
                }

                val payload = IngestionPayload(
                    reportId = observation.observationId,
                    latitude = latitude,
                    longitude = longitude,
                    observedAt = Instant.ofEpochMilli(observation.observedAt).toString(),
                    confidence = observation.confidence,
                    gpsAccuracyM = observation.gpsAccuracy,
                )

                when (val result = client.upload(payload)) {
                    is IngestionResult.Success -> {
                        repository.markSynced(
                            observationId = observation.observationId,
                            serverRecordId = result.potholeId,
                        )
                        Log.i(TAG, "QUEUE ITEM UPDATED report=${observation.observationId} status=SYNCED")
                    }
                    is IngestionResult.Retryable -> {
                        Log.w(TAG, "UPLOAD RETRY report=${observation.observationId}: ${result.message}")
                        retryNeeded = true
                        break
                    }
                    is IngestionResult.PermanentFailure -> {
                        repository.markSyncFailed(observation.observationId)
                        Log.e(TAG, "UPLOAD FAILURE report=${observation.observationId}: ${result.message}")
                    }
                }
            }
        } catch (error: Exception) {
            Log.e(TAG, "UPLOAD FAILURE worker ${error.javaClass.name}: ${error.message}", error)
            throw error
        } finally {
            SyncScheduler.endUpload()
        }

        return if (retryNeeded) Result.retry() else Result.success()
    }

    private companion object {
        const val TAG = "IngestionSync"
        const val MAX_BATCH_SIZE = 50
    }
}
