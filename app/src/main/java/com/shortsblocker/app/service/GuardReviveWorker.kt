package com.shortsblocker.app.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters

/**
 * Retries starting [BlockGuardForegroundService] when starting from the background
 * was restricted by the OS (e.g. ForegroundServiceStartNotAllowedException).
 */
class GuardReviveWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "GuardReviveWorker executing to start BlockGuardForegroundService")
        return runCatching {
            BlockGuardForegroundService.startDirect(applicationContext)
            Result.success()
        }.getOrElse { e ->
            Log.w(TAG, "Failed to start guard service from worker", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "GuardReviveWorker"
        private const val WORK_NAME = "guard_revive_work"
        private const val MAX_RETRIES = 3

        fun schedule(context: Context) {
            runCatching {
                val request = OneTimeWorkRequestBuilder<GuardReviveWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

                WorkManager.getInstance(context)
                    .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
            }.onFailure { e ->
                Log.e(TAG, "Failed to enqueue GuardReviveWorker", e)
            }
        }
    }
}
