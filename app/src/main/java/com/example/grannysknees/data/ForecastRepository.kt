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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * R0 vertical slice: fetch → file cache → score → [TodayUiState].
 *
 * Network failures fall back to a usable (fresh/stale) disk cache when age
 * allows; expired or empty cache yields Unavailable without inventing scores.
 *
 * [fetchedAt] is stamped **after** a successful HTTP body is validated, and
 * presentation always re-reads [clock] so hour/freshness cannot lag the
 * transport (review R2).
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
        val zone = ZoneId.of(timezone)
        try {
            refreshFromNetwork()
        } catch (_: IOException) {
            // Fall through to cache.
        } catch (_: OpenMeteoParseException) {
            // Do not overwrite a good cache with a bad payload.
        }

        // Re-read the clock after the network attempt so presentation uses
        // "now" at evaluation time, not at request start (R2).
        return presentFromCache(clock.instant(), zone)
    }

    /**
     * Re-presents disk cache with a fresh clock — no network.
     *
     * Used for hour/freshness tickers and Activity resume (R1).
     */
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

    /** Delay until the next hour or freshness boundary for the cached envelope. */
    fun delayUntilNextPresentationChange(
        referenceNow: Instant = clock.instant(),
    ): Duration =
        PresentationSchedule.delayUntilNextTransition(
            now = referenceNow,
            fetchedAt = store.read()?.fetchedAt,
            zoneId = ZoneId.of(timezone),
            policy = policy,
        )

    private fun refreshFromNetwork() {
        val request = OpenMeteoRequest(
            latitude = latitude,
            longitude = longitude,
            timezone = timezone,
        )
        val url = client.buildUrl(request)
        val body = transport.get(url)
        // Validate before writing so a bad payload cannot clobber a good cache.
        OpenMeteoParser.parse(body)
        val fetchedAt = clock.instant()
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
