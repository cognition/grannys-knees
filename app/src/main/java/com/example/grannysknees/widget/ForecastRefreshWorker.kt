package com.example.grannysknees.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.grannysknees.data.ForecastCacheStore
import com.example.grannysknees.data.ForecastRepository
import com.example.grannysknees.data.SelectedCityStore
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Refreshes the shared forecast cache, then asks the widget to redraw.
 *
 * The widget itself does not open a network connection. With no saved city
 * the worker leaves the cache alone.
 */
internal class ForecastRefreshWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val cities = SelectedCityStore(File(applicationContext.filesDir, CITY_FILE))
        if (cities.read() == null) return Result.success()
        val repository = ForecastRepository(
            store = ForecastCacheStore(File(applicationContext.filesDir, CACHE_FILE)),
            cityStore = cities,
        )
        repository.loadToday()
        GrannysKneesWidget().updateAll(applicationContext)
        return Result.success()
    }

    internal companion object {
        const val UNIQUE_NAME: String = "forecast-refresh"
        private const val CACHE_FILE: String = "forecast-cache.json"
        private const val CITY_FILE: String = "selected-city.json"

        /** Keeps an hourly refresh across process death and reboot. */
        fun ensureScheduled(context: Context) {
            val request = PeriodicWorkRequestBuilder<ForecastRefreshWorker>(1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
