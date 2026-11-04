package com.example.grannysknees.widget

import android.content.Context
import com.example.grannysknees.data.DefaultLocation
import com.example.grannysknees.data.ForecastCacheStore
import com.example.grannysknees.data.ForecastRepository
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
    /** Creates a loader for the same app-private cache file used by Today. */
    constructor(context: Context) : this(
        repository = ForecastRepository(
            store = ForecastCacheStore(
                File(context.filesDir, CACHE_FILE_NAME),
            ),
        ),
    )

    /** Evaluates and maps the cached forecast at a single fixed instant. */
    fun load(
        referenceNow: Instant = clock.instant(),
        zoneId: ZoneId = ZoneId.of(DefaultLocation.TIMEZONE),
    ): WidgetUiModel =
        mapper.fromToday(
            repository.presentFromCache(
                referenceNow = referenceNow,
                zone = zoneId,
            ),
        )

    private companion object {
        /** Must match the cache file opened by MainActivity. */
        const val CACHE_FILE_NAME: String = "forecast-cache.json"
    }
}
