package com.example.grannysknees

import android.app.Application
import com.example.grannysknees.widget.ForecastRefreshWorker

/**
 * Application entry. Cancels any leftover periodic forecast work so weather
 * is pulled only on first open of the day or explicit refresh.
 */
class GrannysKneesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ForecastRefreshWorker.cancelScheduled(this)
    }
}
