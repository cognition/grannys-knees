package ca.aeo3.grannysknees

import android.app.Application
import ca.aeo3.grannysknees.widget.ForecastRefreshWorker

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
