package com.familysafety.guard.worker

import android.content.Context
import androidx.work.*
import com.familysafety.guard.FamilySafetyApp
import java.util.concurrent.TimeUnit

class OfflineLocationSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? FamilySafetyApp ?: return Result.failure()
        return try {
            val count = app.safetyRepository.flushOfflineQueue()
            Result.success(workDataOf("flushed_count" to count))
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "offline_location_sync_work"

        fun scheduleSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<OfflineLocationSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
        }
    }
}