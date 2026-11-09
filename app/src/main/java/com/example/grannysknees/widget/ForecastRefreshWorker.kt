package com.example.grannysknees.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters

/**
 * Legacy hourly forecast worker.
 *
 * Background pulls are no longer scheduled. [cancelScheduled] removes any
 * leftover unique work from earlier installs. [doWork] is a no-op so a
 * queued instance cannot hit the network.
 */
internal class ForecastRefreshWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = Result.success()

    internal companion object {
        const val UNIQUE_NAME: String = "forecast-refresh"

        /** Drops any leftover periodic forecast refresh from prior versions. */
        fun cancelScheduled(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
