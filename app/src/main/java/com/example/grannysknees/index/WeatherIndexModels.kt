package com.example.grannysknees.index

import java.math.BigDecimal

/**
 * Data model for the **experimental** `index-v1` Arthritis Index scorer.
 *
 * The Arthritis Index is a playful, research-informed 0-10 "joint-weather"
 * indicator derived from public weather forecasts. It is **not medical advice,
 * not a diagnosis, and not a clinically validated prediction** — it only
 * expresses a heuristic signal about how provocative the forecast weather might
 * feel. Correlation is not causation. See `docs/contracts/weather-index.md`
 * (the normative contract) for the full disclaimer and formula.
 *
 * These types are pure Kotlin (no Android, no HTTP, no Compose). Quantised
 * fields are modelled as [BigDecimal] so their decimal scale is explicit and
 * comparable field-for-field against the committed fixtures.
 */

/**
 * A single hourly weather observation fed to the scorer.
 *
 * Timestamps follow the contract's `timeformat=unixtime` model: [epoch] is an
 * integer Unix instant (UTC), and consecutive samples are exactly one real hour
 * apart so the 3-hour pressure trend is a true 3-hour change across DST
 * boundaries (contract §4, §5). A `null` reading means the value is missing and
 * must never be treated as `0` (contract §7).
 */
data class HourlyObservation(
    val epoch: Long,
    val surfacePressureHpa: Double?,
    val relativeHumidityPct: Double?,
    val temperatureC: Double?,
)

/**
 * Input to [WeatherIndexScorer.score].
 *
 * @param zoneId IANA timezone name (e.g. `America/Toronto`). Each [epoch] is
 *   converted with this zone to obtain DST-correct local time and a per-instant
 *   UTC offset (contract §4).
 * @param hours ordered hourly observations, earliest first, one real hour apart.
 */
data class ScoringRequest(
    val zoneId: String,
    val hours: List<HourlyObservation>,
)

/** Echo of the raw hourly inputs for an hour (missing values stay `null`). */
data class HourInputs(
    val surfacePressureHpa: Double?,
    val relativeHumidityPct: Double?,
    val temperatureC: Double?,
)

/**
 * The three `index-v1` sub-scores (each 0-10). A sub-score is `null` when its
 * own input is unavailable; the `pressure` sub-score depends on the 3-hour
 * trend, so it is `null` whenever that trend is undefined (contract §9.1, §7).
 */
data class SubScores(
    val pressure: BigDecimal?,
    val humidity: BigDecimal?,
    val temperature: BigDecimal?,
)

/** Availability of an hour's index (contract §7). */
enum class HourStatus { AVAILABLE, UNAVAILABLE }

/**
 * The scored result for one hour.
 *
 * [pressureTrend3hHpa] is quantised to 2 dp, [subScores] and [indexRaw] to
 * 4 dp, [indexScore] to 1 dp, and [indexDisplay] is an integer — all half-up
 * (contract §9.3-§9.4). When [status] is [HourStatus.UNAVAILABLE] the aggregate
 * index fields are `null` and [unavailableReasons] lists the machine-readable
 * causes; it is empty for available hours.
 */
data class HourIndex(
    val epoch: Long,
    val instantUtc: String,
    val local: String,
    val utcOffsetSeconds: Int,
    val localDate: String,
    val inputs: HourInputs,
    val pressureTrend3hHpa: BigDecimal?,
    val subScores: SubScores,
    val indexRaw: BigDecimal?,
    val indexScore: BigDecimal?,
    val indexDisplay: Int?,
    val status: HourStatus,
    val unavailableReasons: List<String>,
)

/** Reference to a specific hour, used for daily low/high (contract §8.4). */
data class HourRef(
    val epoch: Long,
    val instantUtc: String,
    val local: String,
)

/** Completeness of a local day's index coverage (contract §8.3). */
enum class DailyStatus { COMPLETE, PARTIAL, UNAVAILABLE }

/**
 * Daily low/high of the hourly `indexScore` across one local calendar day
 * (contract §8.2). [low]/[high] are the 1-dp extrema; [lowHour]/[highHour] are
 * resolved by earliest epoch when several hours tie, with [lowTies]/[highTies]
 * exposing how many hours shared each extreme.
 */
data class DailyIndex(
    val date: String,
    val low: BigDecimal,
    val high: BigDecimal,
    val lowHour: HourRef,
    val highHour: HourRef,
    val lowTies: Int,
    val highTies: Int,
    val hoursTotal: Int,
    val hoursAvailable: Int,
    val status: DailyStatus,
    val tieBreak: String,
)

/** Full `index-v1` result: per-hour scores plus the daily low/high outlook. */
data class IndexResult(
    val contractVersion: String,
    val indexVersion: String,
    val hours: List<HourIndex>,
    val daily: List<DailyIndex>,
)
