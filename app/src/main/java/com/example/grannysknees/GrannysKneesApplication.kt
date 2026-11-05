package com.example.grannysknees

import android.app.Application
import com.example.grannysknees.widget.ForecastRefreshWorker

/** Starts the forecast refresh that the home-screen widget reads after reboot. */
class GrannysKneesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ForecastRefreshWorker.ensureScheduled(this)
    }
}
