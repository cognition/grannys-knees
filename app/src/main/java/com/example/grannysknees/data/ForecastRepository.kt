package com.example.grannysknees.data

import com.example.grannysknees.index.WeatherIndexScorer
import com.example.grannysknees.ui.outlook.OutlookUiState
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
import java.time.LocalDate
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
 *
 * Automatic network pulls are gated to the first open of the place's local
 * calendar day unless [forceNetwork] is true (pull-to-refresh or city change).
 */
internal class ForecastRepository(
    private val store: ForecastCacheStore,
    private val transport: HttpTransport = UrlHttpTransport(),
    private val client: OpenMeteoClient = OpenMeteoClient(transport = transport),
    private val clock: Clock = Clock.systemUTC(),
    private val policy: CachePolicy = CachePolicy.CONTRACT_DEFAULT,
    private val cityStore: SelectedCityStore? = null,
    private val fetchStampStore: DailyFetchStampStore? = null,
    private val sensitivityStore: SensitivityProfileStore? = null,
    private val localPressureStore: LocalPressureStore? = null,
    private val feelCalibrationStore: FeelCalibrationStore? = null,
    private val locationLabel: String = DefaultLocation.LABEL,
    private val latitude: Double = DefaultLocation.LATITUDE,
    private val longitude: Double = DefaultLocation.LONGITUDE,
    private val timezone: String = DefaultLocation.TIMEZONE,
) {
    /**
     * Loads Today state, optionally skipping HTTP when today's stamp and a
     * matching cache already exist.
     *
     * Safe to call off the main thread (performs HTTP and file I/O).
     *
     * @param forceNetwork when true, always attempts a forecast HTTP pull
     * (explicit refresh or city change). Default true keeps older callers and
     * tests on the explicit path.
     */
    fun loadToday(forceNetwork: Boolean = true): TodayUiState {
        val place = activePlace()
        if (place == null) {
            return missingCity()
        }
        if (shouldFetchFromNetwork(forceNetwork, place)) {
            try {
                refreshFromNetwork(place)
                stampSuccessfulFetch(place)
            } catch (_: IOException) {
                // Fall through to cache.
            } catch (_: OpenMeteoParseException) {
                // Do not overwrite a good cache with a bad payload.
            }
        }

        // Re-read the clock after the network attempt so presentation uses
        // "now" at evaluation time, not at request start (R2).
        return presentFromCache(clock.instant(), ZoneId.of(place.timezone))
    }

    /**
     * True when a forecast HTTP pull is required for this open.
     *
     * Network is required when forced, when there is no usable cache for the
     * saved place, or when today's local date has not been stamped yet.
     */
    internal fun shouldFetchFromNetwork(
        forceNetwork: Boolean,
        place: SelectedCity,
        referenceNow: Instant = clock.instant(),
    ): Boolean {
        if (forceNetwork) return true
        val envelope = store.read()
        if (envelope == null || !place.matches(envelope)) return true
        val today = LocalDate.ofInstant(referenceNow, ZoneId.of(place.timezone))
        val stamped = fetchStampStore?.readLocalDate()
        return stamped != today
    }

    private fun stampSuccessfulFetch(place: SelectedCity) {
        val stampStore = fetchStampStore ?: return
        val today = LocalDate.ofInstant(clock.instant(), ZoneId.of(place.timezone))
        stampStore.writeLocalDate(today)
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
        val place = activePlace()
        if (place == null) return missingCity()
        val envelope = store.read()
        if (cityStore != null && envelope != null && !place.matches(envelope)) {
            return TodayUiState.Unavailable(
                location = place.label,
                lastUpdatedLabel = "not yet",
                reason = UnavailableReason.EMPTY,
            )
        }
        val status = CacheFreshness.evaluate(envelope?.fetchedAt, referenceNow, policy)
        if (envelope == null || status == CacheStatus.UNAVAILABLE) {
            return TodayUiState.Unavailable(
                location = place.label,
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
                forecastHasSnow = false,
            )
        }

        val forecast = try {
            OpenMeteoParser.parse(envelope.rawBody)
        } catch (_: OpenMeteoParseException) {
            return TodayUiState.Unavailable(
                location = envelope.locationLabel,
                lastUpdatedLabel = TodayMapper.formatFetchedAt(envelope.fetchedAt, zone)
                    ?: "not yet",
                reason = UnavailableReason.EMPTY,
            )
        }

        val request = WeatherToScoringAdapter.toScoringRequest(forecast)
        val scored = WeatherIndexScorer.score(request)
        val personalProfile = resolvePersonalProfile() ?:
            SensitivityProfile.DEFAULT.takeIf { localPressureEnabled() }
        val personalRequest = personalScoringRequest(forecast, referenceNow)
        val personal = personalProfile?.let { profile ->
            WeatherIndexScorer.score(personalRequest, profile.toDeltaSensitivities())
        }
        return TodayMapper.toUiState(
            locationLabel = envelope.locationLabel,
            fetchedAt = envelope.fetchedAt,
            cacheStatus = status,
            indexResult = scored,
            zoneId = forecast.timezone.zone,
            referenceNow = referenceNow,
            personalResult = personal,
            forecastHasSnow = forecast.hours.any { it.weatherCode in SNOW_WEATHER_CODES },
        )
    }

    /**
     * Projects the disk cache into the 24-hour curve and five-day outlook — no network.
     *
     * Follows the same freshness gate as [presentFromCache]: only a fresh or
     * stale cache is shown; expired, empty or unparseable caches are
     * [OutlookUiState.Hidden] so old values are never presented as current.
     */
    fun presentOutlookFromCache(
        referenceNow: Instant = clock.instant(),
    ): OutlookUiState {
        val place = activePlace()
        if (place == null) return OutlookUiState.Hidden
        val envelope = store.read() ?: return OutlookUiState.Hidden
        if (cityStore != null && !place.matches(envelope)) return OutlookUiState.Hidden
        val status = CacheFreshness.evaluate(envelope.fetchedAt, referenceNow, policy)
        if (status != CacheStatus.FRESH && status != CacheStatus.STALE) {
            return OutlookUiState.Hidden
        }
        val forecast = try {
            OpenMeteoParser.parse(envelope.rawBody)
        } catch (_: OpenMeteoParseException) {
            return OutlookUiState.Hidden
        }
        val request = WeatherToScoringAdapter.toScoringRequest(forecast)
        val scored = WeatherIndexScorer.score(request)
        val personalProfile = resolvePersonalProfile() ?:
            SensitivityProfile.DEFAULT.takeIf { localPressureEnabled() }
        val personalRequest = personalScoringRequest(forecast, referenceNow)
        val personal = personalProfile?.let { profile ->
            WeatherIndexScorer.score(personalRequest, profile.toDeltaSensitivities())
        }
        return OutlookMapper.toOutlook(
            indexResult = scored,
            zoneId = forecast.timezone.zone,
            referenceNow = referenceNow,
            personalResult = personal,
        )
    }

    /**
     * Weather context for a Feel save from the disk cache — no network.
     *
     * Returns a capture with a null window mean when the cache cannot supply
     * AVAILABLE hours in the ±3h window.
     */
    fun captureFeelWeather(at: Instant = clock.instant()): FeelWeatherCapture {
        val place = activePlace() ?: return FeelWeatherCapture(
            weatherWindowMean = null,
            deltaPressure = null,
            deltaHumidity = null,
            deltaTemperature = null,
        )
        val envelope = store.read() ?: return FeelWeatherCapture(
            weatherWindowMean = null,
            deltaPressure = null,
            deltaHumidity = null,
            deltaTemperature = null,
        )
        if (cityStore != null && !place.matches(envelope)) {
            return FeelWeatherCapture(
                weatherWindowMean = null,
                deltaPressure = null,
                deltaHumidity = null,
                deltaTemperature = null,
            )
        }
        val forecast = try {
            OpenMeteoParser.parse(envelope.rawBody)
        } catch (_: OpenMeteoParseException) {
            return FeelWeatherCapture(
                weatherWindowMean = null,
                deltaPressure = null,
                deltaHumidity = null,
                deltaTemperature = null,
            )
        }
        val scored = WeatherIndexScorer.score(
            WeatherToScoringAdapter.toScoringRequest(forecast),
        )
        return FeelWeatherWindow.capture(scored.hours, at)
    }

    /** Effective Personal multipliers from Adj + Feel samples, or null. */
    private fun resolvePersonalProfile(): SensitivityProfile? {
        val prefs = sensitivityStore?.readState() ?: SensitivityPreferences.DEFAULT
        val samples = feelCalibrationStore?.read().orEmpty()
        return PersonalIndexResolver.effectiveProfile(
            adj = prefs.profile,
            feelOptIn = prefs.feelCalibrationEnabled,
            samples = samples,
        )
    }

    private fun personalScoringRequest(
        forecast: com.example.grannysknees.weather.HourlyForecast,
        referenceNow: Instant,
    ) = if (sensitivityStore?.readLocalPressureEnabled() == true) {
        val blended = LocalPressureBlend.pressureForCurrentHour(
            forecast = forecast,
            reading = localPressureStore?.read(),
            now = referenceNow,
        )
        WeatherToScoringAdapter.toScoringRequest(
            forecast = forecast,
            localPressureHpa = blended,
            referenceNow = referenceNow,
        )
    } else {
        WeatherToScoringAdapter.toScoringRequest(forecast)
    }

    private fun localPressureEnabled(): Boolean =
        sensitivityStore?.readLocalPressureEnabled() == true

    /** Delay until the next hour or freshness boundary for the cached envelope. */
    fun delayUntilNextPresentationChange(
        referenceNow: Instant = clock.instant(),
    ): Duration =
        PresentationSchedule.delayUntilNextTransition(
            now = referenceNow,
            fetchedAt = store.read()?.fetchedAt,
            zoneId = ZoneId.of(activePlace()?.timezone ?: timezone),
            policy = policy,
        )

    private fun refreshFromNetwork(place: SelectedCity) {
        val request = OpenMeteoRequest(
            latitude = place.latitude,
            longitude = place.longitude,
            timezone = place.timezone,
        )
        val url = client.buildUrl(request)
        val body = transport.get(url)
        // Validate before writing so a bad payload cannot clobber a good cache.
        OpenMeteoParser.parse(body)
        val fetchedAt = clock.instant()
        store.write(
            ForecastCacheEnvelope(
                fetchedAt = fetchedAt,
                locationLabel = place.label,
                latitude = place.latitude,
                longitude = place.longitude,
                timezone = place.timezone,
                rawBody = body,
            ),
        )
    }

    /**
     * Saved city when a store is attached. Constructor coordinates otherwise,
     * so existing tests keep requesting Toronto.
     */
    private fun activePlace(): SelectedCity? {
        val store = cityStore ?: return SelectedCity(
            label = locationLabel,
            latitude = latitude,
            longitude = longitude,
            timezone = timezone,
            admin1 = "",
            countryCode = "CA",
        )
        return store.read()
    }

    private fun missingCity(): TodayUiState.Unavailable = TodayUiState.Unavailable(
        location = "",
        lastUpdatedLabel = "not yet",
        reason = UnavailableReason.EMPTY,
    )

    private companion object {
        /** Open-Meteo WMO codes for snow, snow grains, and snow showers. */
        val SNOW_WEATHER_CODES = setOf(71, 73, 75, 77, 85, 86)
    }
}
