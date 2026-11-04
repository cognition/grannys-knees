package com.example.grannysknees.index

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Pure-Kotlin implementation of the **experimental** `index-v1` Arthritis Index
 * (contract `docs/contracts/weather-index.md`, §9). It is a transparent,
 * versioned heuristic — **not medical advice, a diagnosis, or a clinically
 * validated prediction.** The scorer makes no causal or medical claim; it only
 * converts forecast weather into a 0-10 "joint-weather" signal.
 *
 * The scorer is self-contained: it performs its own `unixtime` + IANA timezone
 * conversion (contract §4) from each [HourlyObservation.epoch], so it does not
 * depend on the weather client. It does no I/O, no HTTP, and no Android work.
 *
 * Arithmetic is done in exact [BigDecimal] (divisions at [MathContext.DECIMAL128]),
 * and quantisation is applied half-up to the decimal value rather than a binary
 * float (contract §9.4.2) — so a decimal like `5.25` rounds to `5.3`, not `5.2`.
 * Equality against the fixtures is therefore exact on the canonical quantised
 * values, with no floating-point tolerance.
 */
object WeatherIndexScorer {

    const val CONTRACT_VERSION: String = "weather-index/1.0.0"
    const val INDEX_VERSION: String = "index-v1"

    /** Reported tie-break rule for daily low/high (contract §8.4). */
    const val TIE_BREAK: String = "earliest epoch"

    // --- index-v1 constants (experimental anchors; see contract §9.1-§9.2, §11) ---
    private const val TREND_LAG = 3                      // positions = exactly 3 real hours (§5.1)
    private val PRESSURE_SCALE_HPA = BigDecimal("6")     // |3-h change| mapped onto 0-10 (§9.1)
    private val HUMIDITY_OFFSET = BigDecimal("40")
    private val HUMIDITY_SPAN = BigDecimal("50")
    private val TEMP_BASELINE_C = BigDecimal("20")       // colder than this scores higher
    private val TEMP_SPAN = BigDecimal("20")
    private val WEIGHT_PRESSURE = BigDecimal("0.50")
    private val WEIGHT_HUMIDITY = BigDecimal("0.25")
    private val WEIGHT_TEMPERATURE = BigDecimal("0.25")
    private const val COMPLETE_DAY_MIN_AVAILABLE_HOURS = 18 // contract §8.3 (reviewable default)

    // --- quantisation scales (contract §9.3-§9.4) ---
    private const val SCALE_TREND = 2
    private const val SCALE_SUBSCORE = 4
    private const val SCALE_INDEX_RAW = 4
    private const val SCALE_INDEX_SCORE = 1

    // --- machine-readable unavailability reasons (contract §7) ---
    private const val REASON_PRESSURE_NULL = "surface_pressure null"
    private const val REASON_HUMIDITY_NULL = "relative_humidity_2m null"
    private const val REASON_TEMPERATURE_NULL = "temperature_2m null"
    private const val REASON_TREND = "pressureTrend3h unavailable (no comparable t-3h sample)"

    private val MC = MathContext.DECIMAL128
    private val SCALE_TEN = BigDecimal.TEN
    private val CLAMP_MIN = BigDecimal.ZERO
    private val CLAMP_MAX = BigDecimal.TEN

    private val LOCAL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")
    private val UTC_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

    /** Score every hour and derive the daily low/high outlook. */
    fun score(request: ScoringRequest): IndexResult {
        val zone = ZoneId.of(request.zoneId)
        val samples = request.hours
        val hours = samples.indices.map { i -> scoreHour(samples, i, zone) }
        return IndexResult(
            contractVersion = CONTRACT_VERSION,
            indexVersion = INDEX_VERSION,
            hours = hours,
            daily = aggregateDaily(hours),
        )
    }

    private fun scoreHour(samples: List<HourlyObservation>, i: Int, zone: ZoneId): HourIndex {
        val sample = samples[i]
        val instant = Instant.ofEpochSecond(sample.epoch)
        val zoned = instant.atZone(zone)

        val pressure = sample.surfacePressureHpa?.let { BigDecimal.valueOf(it) }
        val humidity = sample.relativeHumidityPct?.let { BigDecimal.valueOf(it) }
        val temperature = sample.temperatureC?.let { BigDecimal.valueOf(it) }

        // 3-hour pressure trend from comparable timestamps (contract §5.1, §7).
        val priorPressure =
            if (i >= TREND_LAG) samples[i - TREND_LAG].surfacePressureHpa?.let { BigDecimal.valueOf(it) } else null
        val trend: BigDecimal? =
            if (i >= TREND_LAG && pressure != null && priorPressure != null) pressure - priorPressure else null

        // Sub-scores: each computed independently, clamped to 0-10 (contract §9.1).
        val pressureSub = trend?.let { clamp(it.abs().multiply(SCALE_TEN).divide(PRESSURE_SCALE_HPA, MC)) }
        val humiditySub =
            humidity?.let { clamp((it - HUMIDITY_OFFSET).divide(HUMIDITY_SPAN, MC).multiply(SCALE_TEN)) }
        val temperatureSub =
            temperature?.let { clamp((TEMP_BASELINE_C - it).divide(TEMP_SPAN, MC).multiply(SCALE_TEN)) }

        // indexRaw uses full-precision (unrounded) sub-scores (contract §9.2, §9.4.1).
        val indexRawValue: BigDecimal? =
            if (pressureSub != null && humiditySub != null && temperatureSub != null) {
                pressureSub.multiply(WEIGHT_PRESSURE)
                    .add(humiditySub.multiply(WEIGHT_HUMIDITY))
                    .add(temperatureSub.multiply(WEIGHT_TEMPERATURE))
            } else {
                null
            }

        val available = indexRawValue != null
        val reasons = if (available) emptyList() else buildReasons(sample, trend)

        return HourIndex(
            epoch = sample.epoch,
            instantUtc = UTC_FORMAT.format(instant),
            local = LOCAL_FORMAT.format(zoned),
            utcOffsetSeconds = zoned.offset.totalSeconds,
            localDate = zoned.toLocalDate().toString(),
            inputs = HourInputs(
                surfacePressureHpa = sample.surfacePressureHpa,
                relativeHumidityPct = sample.relativeHumidityPct,
                temperatureC = sample.temperatureC,
            ),
            pressureTrend3hHpa = trend?.setScale(SCALE_TREND, RoundingMode.HALF_UP),
            subScores = SubScores(
                pressure = pressureSub?.setScale(SCALE_SUBSCORE, RoundingMode.HALF_UP),
                humidity = humiditySub?.setScale(SCALE_SUBSCORE, RoundingMode.HALF_UP),
                temperature = temperatureSub?.setScale(SCALE_SUBSCORE, RoundingMode.HALF_UP),
            ),
            indexRaw = indexRawValue?.setScale(SCALE_INDEX_RAW, RoundingMode.HALF_UP),
            indexScore = indexRawValue?.setScale(SCALE_INDEX_SCORE, RoundingMode.HALF_UP),
            indexDisplay = indexRawValue?.setScale(0, RoundingMode.HALF_UP)?.intValueExact(),
            status = if (available) HourStatus.AVAILABLE else HourStatus.UNAVAILABLE,
            unavailableReasons = reasons,
        )
    }

    private fun buildReasons(sample: HourlyObservation, trend: BigDecimal?): List<String> =
        buildList {
            if (sample.surfacePressureHpa == null) add(REASON_PRESSURE_NULL)
            if (sample.relativeHumidityPct == null) add(REASON_HUMIDITY_NULL)
            if (sample.temperatureC == null) add(REASON_TEMPERATURE_NULL)
            if (trend == null) add(REASON_TREND)
        }

    /**
     * Daily low/high from the hourly `indexScore` (contract §8.2-§8.4), grouped
     * by local calendar date and over available hours only. Local dates with no
     * available hours (e.g. lead-in hours whose trend is undefined) are omitted,
     * matching the committed fixtures.
     */
    private fun aggregateDaily(hours: List<HourIndex>): List<DailyIndex> =
        hours.groupBy { it.localDate }.entries.mapNotNull { (date, dayHours) ->
            val available = dayHours.filter { it.indexScore != null }
            if (available.isEmpty()) return@mapNotNull null

            val scores = available.map { it.indexScore!! }
            val lowValue = scores.minWith(naturalOrder())
            val highValue = scores.maxWith(naturalOrder())

            val lowTied = available.filter { it.indexScore!!.compareTo(lowValue) == 0 }
            val highTied = available.filter { it.indexScore!!.compareTo(highValue) == 0 }
            val lowHour = lowTied.minByOrNull { it.epoch }!!
            val highHour = highTied.minByOrNull { it.epoch }!!

            DailyIndex(
                date = date,
                low = lowValue,
                high = highValue,
                lowHour = lowHour.toRef(),
                highHour = highHour.toRef(),
                lowTies = lowTied.size,
                highTies = highTied.size,
                hoursTotal = dayHours.size,
                hoursAvailable = available.size,
                status = if (available.size >= COMPLETE_DAY_MIN_AVAILABLE_HOURS) {
                    DailyStatus.COMPLETE
                } else {
                    DailyStatus.PARTIAL
                },
                tieBreak = TIE_BREAK,
            )
        }

    private fun HourIndex.toRef() = HourRef(epoch = epoch, instantUtc = instantUtc, local = local)

    private fun clamp(value: BigDecimal): BigDecimal = value.coerceIn(CLAMP_MIN, CLAMP_MAX)
}
