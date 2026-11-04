package com.example.grannysknees.data

import com.example.grannysknees.index.WeatherIndexScorer
import com.example.grannysknees.ui.today.TodayUiState
import com.example.grannysknees.ui.today.UnavailableReason
import com.example.grannysknees.weather.HttpTransport
import com.example.grannysknees.weather.OpenMeteoClient
import com.example.grannysknees.weather.OpenMeteoParseException
import com.example.grannysknees.weather.OpenMeteoParser
import com.example.grannysknees.weather.OpenMeteoRequest
import com.example.grannysknees.weather.UrlHttpTransport
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * R0 vertical slice: fetch → file cache → score → [TodayUiState].
 *
 * Network failures fall back to a usable (fresh/stale) disk cache when age
 * allows; expired or empty cache yields Unavailable without inventing scores.
 */
internal class ForecastRepository(
    private val store: ForecastCacheStore,
    private val transport: HttpTransport = UrlHttpTransport(),
    private val client: OpenMeteoClient = OpenMeteoClient(transport = transport),
    private val clock: Clock = Clock.systemUTC(),
    private val policy: CachePolicy = CachePolicy.CONTRACT_DEFAULT,
    private val locationLabel: String = DefaultLocation.LABEL,
    private val latitude: Double = DefaultLocation.LATITUDE,
    private val longitude: Double = DefaultLocation.LONGITUDE,
    private val timezone: String = DefaultLocation.TIMEZONE,
) {
    /**
     * Loads Today state: attempts a network refresh, then presents cache + score.
     *
     * Safe to call off the main thread (performs HTTP and file I/O).
     */
    fun loadToday(): TodayUiState {
        val now = clock.instant()
        val zone = ZoneId.of(timezone)
        try {
            refreshFromNetwork(now)
        } catch (_: IOException) {
            // Fall through to cache.
        } catch (_: OpenMeteoParseException) {
            // Do not overwrite a good cache with a bad payload.
        }

        return presentFromCache(now, zone)
    }

    /** Presents disk cache only — used by tests and offline paths. */
    fun presentFromCache(
        referenceNow: Instant = clock.instant(),
        zone: ZoneId = ZoneId.of(timezone),
    ): TodayUiState {
        val envelope = store.read()
        val status = CacheFreshness.evaluate(envelope?.fetchedAt, referenceNow, policy)
        if (envelope == null || status == CacheStatus.UNAVAILABLE) {
            return TodayUiState.Unavailable(
                location = locationLabel,
                lastUpdatedLabel = "not yet",
                reason = UnavailableReason.EMPTY,
            )
        }
        if (status == CacheStatus.EXPIRED) {
            return TodayMapper.toUiState(
                locationLabel = envelope.locationLabel,
                fetchedAt = envelope.fetchedAt,
                cacheStatus = status,
                indexResult = null,
                zoneId = zone,
                referenceNow = referenceNow,
            )
        }

        val forecast = try {
            OpenMeteoParser.parse(envelope.rawBody)
        } catch (_: OpenMeteoParseException) {
            return TodayUiState.Unavailable(
                location = locationLabel,
                lastUpdatedLabel = TodayMapper.formatFetchedAt(envelope.fetchedAt, zone)
                    ?: "not yet",
                reason = UnavailableReason.EMPTY,
            )
        }

        val scored = WeatherIndexScorer.score(WeatherToScoringAdapter.toScoringRequest(forecast))
        return TodayMapper.toUiState(
            locationLabel = envelope.locationLabel,
            fetchedAt = envelope.fetchedAt,
            cacheStatus = status,
            indexResult = scored,
            zoneId = forecast.timezone.zone,
            referenceNow = referenceNow,
        )
    }

    private fun refreshFromNetwork(fetchedAt: Instant) {
        val request = OpenMeteoRequest(
            latitude = latitude,
            longitude = longitude,
            timezone = timezone,
        )
        val url = client.buildUrl(request)
        val body = transport.get(url)
        // Validate before writing so a bad payload cannot clobber a good cache.
        OpenMeteoParser.parse(body)
        store.write(
            ForecastCacheEnvelope(
                fetchedAt = fetchedAt,
                locationLabel = locationLabel,
                latitude = latitude,
                longitude = longitude,
                timezone = timezone,
                rawBody = body,
            ),
        )
    }
}
