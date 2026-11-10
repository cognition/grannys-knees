package com.example.grannysknees.widget

import android.content.Context
import com.example.grannysknees.data.DefaultLocation
import com.example.grannysknees.data.FeelCalibrationStore
import com.example.grannysknees.data.ForecastCacheStore
import com.example.grannysknees.data.ForecastRepository
import com.example.grannysknees.data.LocalPressureStore
import com.example.grannysknees.data.SelectedCityStore
import com.example.grannysknees.data.SensitivityProfileStore
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Reads the app's forecast cache through [ForecastRepository.presentFromCache].
 *
 * This keeps widget evaluation on the same score and freshness path as Today;
 * it performs no network request and never manufactures an offline score.
 */
internal class WidgetModelLoader(
    private val repository: ForecastRepository,
    private val mapper: WidgetModelMapper = WidgetModelMapper(),
    private val clock: Clock = Clock.systemUTC(),
) {
    /** Creates a loader for the same app-private cache and personal stores as Today. */
    constructor(context: Context) : this(
        repository = ForecastRepository(
            store = ForecastCacheStore(
                File(context.filesDir, CACHE_FILE_NAME),
            ),
            cityStore = SelectedCityStore(
                File(context.filesDir, CITY_FILE_NAME),
            ),
            sensitivityStore = SensitivityProfileStore(
                File(context.filesDir, SENSITIVITY_FILE_NAME),
            ),
            localPressureStore = LocalPressureStore(
                File(context.filesDir, LOCAL_PRESSURE_FILE_NAME),
            ),
            feelCalibrationStore = FeelCalibrationStore(
                File(context.filesDir, FEEL_CALIBRATION_FILE_NAME),
            ),
        ),
    )

    /**
     * Evaluates and maps the cached forecast at a single fixed instant.
     *
     * The current score and the outlook both come from [ForecastRepository],
     * so the widget cannot invent an hourly or daily number the phone hid.
     */
    fun load(
        referenceNow: Instant = clock.instant(),
        zoneId: ZoneId = ZoneId.of(DefaultLocation.TIMEZONE),
    ): WidgetUiModel =
        mapper.from(
            state = repository.presentFromCache(
                referenceNow = referenceNow,
                zone = zoneId,
            ),
            outlook = repository.presentOutlookFromCache(referenceNow = referenceNow),
        )

    private companion object {
        /** Must match the cache files opened by MainActivity. */
        const val CACHE_FILE_NAME: String = "forecast-cache.json"
        const val CITY_FILE_NAME: String = "selected-city.json"
        const val SENSITIVITY_FILE_NAME: String = "sensitivity-profile.json"
        const val LOCAL_PRESSURE_FILE_NAME: String = "local-pressure.json"
        const val FEEL_CALIBRATION_FILE_NAME: String = "feel-calibration.json"
    }
}
