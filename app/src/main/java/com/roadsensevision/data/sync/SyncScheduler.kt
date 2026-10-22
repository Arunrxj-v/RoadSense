package com.roadsensevision.data.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SyncScheduler {
    private const val TAG = "IngestionSync"
    private const val PERIODIC_WORK = "roadsense-ingestion-periodic"
    private const val IMMEDIATE_WORK = "roadsense-ingestion-now"

    private val activeWorkers = AtomicInteger(0)
    private val _uploading = MutableStateFlow(false)

    /** True while an ingestion worker is actively sending queued observations. */
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

    internal fun beginUpload() {
        activeWorkers.incrementAndGet()
        _uploading.value = true
        Log.i(TAG, "UPLOAD START activeWorkers=${activeWorkers.get()}")
    }

    internal fun endUpload() {
        val remaining = activeWorkers.decrementAndGet()
        if (remaining <= 0) {
            activeWorkers.set(0)
            _uploading.value = false
        }
        Log.i(TAG, "UPLOAD END activeWorkers=${remaining.coerceAtLeast(0)}")
    }

    fun ensureScheduled(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<IngestionSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun requestImmediateSync(context: Context, force: Boolean = false) {
        if (!IngestionClient().isConfigured) {
            Log.w(TAG, "SYNC SKIPPED ingestion API or key not configured")
            return
        }
        val request = OneTimeWorkRequestBuilder<IngestionSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()
        val policy = if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK,
            policy,
            request,
        )
        Log.i(TAG, "SYNC ENQUEUED uniqueWork=$IMMEDIATE_WORK policy=$policy")
    }
}
