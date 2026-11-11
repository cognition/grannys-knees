package com.aetherealmind.grannysknees.weather

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Timezone-aware domain models for the Open-Meteo hourly forecast, aligned with
 * `docs/contracts/weather-index.md` (contract `weather-index/1.0.0`).
 *
 * These models cover T03's scope only: the provider payload parsed into
 * DST-correct, instant-based hourly rows. They deliberately carry **no** index
 * score, sub-scores, pressure trend, daily low/high, or cache freshness — those
 * are derived by T04 (scorer) and the repository/cache layer, not the client.
 *
 * Missing hourly values are represented as `null` (Kotlin nullable), never as
 * `0` (contract §7).
 */

/** Geographic location echoed by the provider for the forecast. */
data class ForecastLocation(
    val latitude: Double,
    val longitude: Double,
    /** Metres above sea level; `null` if the provider omits it. */
    val elevation: Double?,
)

/**
 * Timezone metadata as returned by the provider.
 *
 * Per contract §4.1, [utcOffsetSecondsAtRequest] and [abbreviationAtRequest]
 * describe the **request-time** offset only and must **not** be applied per-row
 * across a DST boundary. Per-row offsets come from IANA conversion of each
 * epoch (see [ForecastHour.utcOffsetSeconds]).
 */
data class ForecastTimezone(
    /** The IANA zone used for per-instant conversion, e.g. `America/Toronto`. */
    val zone: ZoneId,
    /** Provider abbreviation at request time, e.g. `EST`. */
    val abbreviationAtRequest: String?,
    /** Provider `utc_offset_seconds` at request time. */
    val utcOffsetSecondsAtRequest: Int,
)

/**
 * Units echoed by the provider under `hourly_units`. The parser asserts these
 * match the metric units the contract fixes (°C, %, hPa) rather than assuming
 * them (contract §3.2).
 */
data class HourlyUnits(
    val time: String,
    val temperature2m: String,
    val relativeHumidity2m: String,
    val surfacePressure: String,
    val weatherCode: String?,
)

/**
 * A single hourly row at a genuine UTC instant, one real hour apart from its
 * neighbours (contract §4.1).
 *
 * The timestamp fields are derived by converting [epoch] through the forecast's
 * IANA [ForecastTimezone.zone], so [local], [utcOffsetSeconds] and [localDate]
 * are DST-correct and disambiguate repeated/absent wall-clock labels across a
 * daylight-saving transition (contract §4.2).
 *
 * Weather inputs are nullable: a provider `null` is preserved as `null`, never
 * substituted with `0` (contract §7).
 */
data class ForecastHour(
    /** UTC epoch seconds, exactly as supplied by `hourly.time[]`. */
    val epoch: Long,
    /** The same instant as [epoch]; `instant.toString()` is the contract `instantUtc`. */
    val instant: Instant,
    /** DST-correct local wall time with the offset for this instant. */
    val local: ZonedDateTime,
    /** The UTC offset in seconds **for this instant** (differs across DST). */
    val utcOffsetSeconds: Int,
    /** Local calendar date used for daily low/high membership (contract §4.1.4). */
    val localDate: LocalDate,
    /** Air temperature at 2 m in °C, or `null` if missing. */
    val temperature2m: Double?,
    /** Relative humidity at 2 m in %, or `null` if missing. */
    val relativeHumidity2m: Int?,
    /** Surface (station-level) pressure in hPa, or `null` if missing. */
    val surfacePressure: Double?,
    /** WMO weather interpretation code, or `null` when unavailable. */
    val weatherCode: Int?,
)

/**
 * A parsed Open-Meteo hourly forecast: provider metadata plus the instant-based
 * hourly series. This is the complete T03 deliverable handed to downstream
 * scoring (T04) and UI (T05).
 */
data class HourlyForecast(
    val location: ForecastLocation,
    val timezone: ForecastTimezone,
    /** Open-Meteo `generation_time_ms`, retained for diagnostics (contract §6). */
    val generationTimeMs: Double?,
    val units: HourlyUnits,
    /** Hourly rows in provider order (ascending epoch, one real hour apart). */
    val hours: List<ForecastHour>,
)
