package com.aetherealmind.grannysknees.index

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Slope-index weather sensitivity scorer (`docs/contracts/slope-index.md`).
 *
 * Scores how conditions are *changing* (temperature / humidity / feels-like gap
 * slopes over four hours, plus the 12-hour pressure change), not absolute
 * levels. Maths match `scripts/slope_index.py`. Not medical advice.
 */
object WeatherIndexScorer {

    const val CONTRACT_VERSION: String = "slope-index/1.0.0"
    const val INDEX_VERSION: String = "index-slope-v1"

    /** Reported tie-break rule for daily low/high. */
    const val TIE_BREAK: String = "earliest epoch"

    /**
     * When true, second-derivative acceleration is computed for logging only
     * and never added to the score.
     */
    const val ENABLE_ACCEL_EXPERIMENT: Boolean = false

    private const val PERSONAL_INDEX_VERSION: String = "index-slope-v1-personal"
    private const val HOUR_SECONDS: Long = 3_600L
    private const val SLOPE_POINTS: Int = 4
    private const val PRESSURE_POINTS: Int = 13
    private const val COMPLETE_DAY_MIN_AVAILABLE_HOURS = 18

    private const val SCALE_DELTA = 2
    private const val SCALE_INDEX_RAW = 4
    private const val SCALE_INDEX_SCORE = 1

    private const val REASON_PRESSURE_NULL = "surface_pressure null"
    private const val REASON_HUMIDITY_NULL = "relative_humidity_2m null"
    private const val REASON_TEMPERATURE_NULL = "temperature_2m null"
    private const val REASON_SLOPE_WINDOW =
        "4-hour slope window unavailable (missing contiguous hourly samples)"
    private const val REASON_PRESSURE_WINDOW =
        "12-hour pressure window unavailable (missing contiguous hourly samples)"

    private val MC = MathContext.DECIMAL128
    private val TWO = BigDecimal("2")

    private val LOCAL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")
    private val UTC_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

    /** Score every hour with baseline (neutral) sensitivities and default config. */
    fun score(request: ScoringRequest): IndexResult =
        score(request, DeltaSensitivities.BASELINE, SlopeIndexConfig.DEFAULT)

    /**
     * Score every hour, optionally applying personal influence weights to the
     * pressure, humidity, and cool/warm temperature terms.
     */
    internal fun score(
        request: ScoringRequest,
        sensitivities: DeltaSensitivities,
        config: SlopeIndexConfig = SlopeIndexConfig.DEFAULT,
    ): IndexResult {
        val zone = ZoneId.of(request.zoneId)
        val byEpoch = request.hours.associateBy { it.epoch }
        val hours = request.hours.map { sample ->
            scoreHour(sample, byEpoch, zone, sensitivities, config)
        }
        return IndexResult(
            contractVersion = CONTRACT_VERSION,
            indexVersion = if (sensitivities.isBaseline()) {
                INDEX_VERSION
            } else {
                PERSONAL_INDEX_VERSION
            },
            hours = hours,
            daily = aggregateDaily(hours),
        )
    }

    /**
     * Least-squares slope per hour through four hourly points, oldest first.
     *
     * `m = (-3*x0 - x1 + x2 + 3*x3) / 10`
     */
    internal fun slope4(x0: Double, x1: Double, x2: Double, x3: Double): Double =
        (-3.0 * x0 - x1 + x2 + 3.0 * x3) / 10.0

    /** Zeroes a slope whose absolute value is below [band]. */
    internal fun deadband(value: Double, band: Double): Double =
        if (kotlin.math.abs(value) < band) 0.0 else value

    /** Clamps [value] into [[lo], [hi]]. */
    internal fun clamp(value: Double, lo: Double, hi: Double): Double =
        value.coerceIn(lo, hi)

    /**
     * Pure slope score matching `scripts/slope_index.py` (no personal weights).
     *
     * Used by acceptance tests. [feels4] may be null to force `g = 0`.
     */
    internal fun scoreSlope(
        temp4: List<Double>,
        hum4: List<Double>,
        feels4: List<Double>?,
        pressure13: List<Double>,
        config: SlopeIndexConfig = SlopeIndexConfig.DEFAULT,
    ): Double {
        require(temp4.size == SLOPE_POINTS)
        require(hum4.size == SLOPE_POINTS)
        require(pressure13.size == PRESSURE_POINTS)
        val mT = deadband(slope4(temp4[0], temp4[1], temp4[2], temp4[3]), config.deadbandT)
        val mH = deadband(slope4(hum4[0], hum4[1], hum4[2], hum4[3]), config.deadbandH)
        val g = if (feels4 == null || feels4.size != SLOPE_POINTS) {
            0.0
        } else {
            val gap = temp4.zip(feels4) { t, f -> maxOf(0.0, t - f) }
            deadband(slope4(gap[0], gap[1], gap[2], gap[3]), config.deadbandG)
        }
        val dP12 = pressure13.last() - pressure13.first()
        return assembleScore(
            mT = mT,
            mH = mH,
            g = g,
            dP12 = dP12,
            sensitivities = DeltaSensitivities.BASELINE,
            config = config,
        )
    }

    /**
     * Optional second derivative (rate of the rate of change). Not in the score.
     */
    internal fun acceleration(slopeNow: Double, slopeOneHourAgo: Double): Double =
        slopeNow - slopeOneHourAgo

    /**
     * Scales one signed contribution by an influence multiplier.
     *
     * Positive contributions (toward discomfort) are multiplied by [sensitivity].
     * Negative (relief) contributions are multiplied by `2 − sensitivity`.
     */
    internal fun scaleContribution(
        contribution: BigDecimal,
        sensitivity: BigDecimal,
    ): BigDecimal {
        val sign = contribution.signum()
        if (sign == 0) return BigDecimal.ZERO
        return if (sign > 0) {
            contribution.multiply(sensitivity, MC)
        } else {
            val reliefFactor = TWO.subtract(sensitivity, MC)
            contribution.multiply(reliefFactor, MC)
        }
    }

    private fun scaleTerm(contribution: Double, sensitivity: BigDecimal): Double =
        scaleContribution(BigDecimal.valueOf(contribution), sensitivity).toDouble()

    private fun assembleScore(
        mT: Double,
        mH: Double,
        g: Double,
        dP12: Double,
        sensitivities: DeltaSensitivities,
        config: SlopeIndexConfig,
    ): Double {
        val pressureTerm = scaleTerm(-config.pressureW * dP12, sensitivities.pressure)
        val coolTerm = scaleTerm(config.coolW * maxOf(0.0, -mT), sensitivities.temperature)
        val warmTerm = scaleTerm(config.warmW * maxOf(0.0, mT), sensitivities.temperature)
        val humidTerm = scaleTerm(config.humidW * mH, sensitivities.humidity)
        val gapTerm = clamp(config.gapW * g, -config.gapClamp, config.gapClamp)
        val raw = config.baseline + pressureTerm + coolTerm + warmTerm + humidTerm + gapTerm
        return clamp(raw, config.lo, config.hi)
    }

    private fun scoreHour(
        sample: HourlyObservation,
        byEpoch: Map<Long, HourlyObservation>,
        zone: ZoneId,
        sensitivities: DeltaSensitivities,
        config: SlopeIndexConfig,
    ): HourIndex {
        val instant = Instant.ofEpochSecond(sample.epoch)
        val zoned = instant.atZone(zone)

        val slopeWindow = window(sample.epoch, SLOPE_POINTS, byEpoch)
        val pressureWindow = window(sample.epoch, PRESSURE_POINTS, byEpoch)

        val temp4 = slopeWindow?.map { it.temperatureC }
        val hum4 = slopeWindow?.map { it.relativeHumidityPct }
        val feels4 = slopeWindow?.map { it.feelsLikeC }
        val pressure13 = pressureWindow?.map { it.surfacePressureHpa }

        val tempsReady = temp4 != null && temp4.all { it != null }
        val humsReady = hum4 != null && hum4.all { it != null }
        // Slope-index contract: dP12 only needs the 12 h pressure endpoints.
        val pressuresReady = pressure13 != null &&
            pressure13.first() != null &&
            pressure13.last() != null

        val mT: Double?
        val mH: Double?
        val g: Double?
        val dP12: Double?
        val indexUnrounded: Double?

        if (tempsReady && humsReady && pressuresReady) {
            val temps = temp4!!.map { it!! }
            val hums = hum4!!.map { it!! }
            mT = deadband(slope4(temps[0], temps[1], temps[2], temps[3]), config.deadbandT)
            mH = deadband(slope4(hums[0], hums[1], hums[2], hums[3]), config.deadbandH)
            g = if (feels4 == null || feels4.any { it == null }) {
                0.0
            } else {
                val gap = temps.zip(feels4.map { it!! }) { t, f -> maxOf(0.0, t - f) }
                deadband(slope4(gap[0], gap[1], gap[2], gap[3]), config.deadbandG)
            }
            dP12 = pressure13!!.last()!! - pressure13.first()!!
            indexUnrounded = assembleScore(
                mT = mT,
                mH = mH,
                g = g,
                dP12 = dP12,
                sensitivities = sensitivities,
                config = config,
            )
            if (ENABLE_ACCEL_EXPERIMENT && slopeWindow != null) {
                val priorEpoch = sample.epoch - HOUR_SECONDS
                val priorSlopeWindow = window(priorEpoch, SLOPE_POINTS, byEpoch)
                val priorTemps = priorSlopeWindow?.map { it.temperatureC }
                if (priorTemps != null && priorTemps.all { it != null }) {
                    val p = priorTemps.map { it!! }
                    val mTPrev = deadband(
                        slope4(p[0], p[1], p[2], p[3]),
                        config.deadbandT,
                    )
                    // Experiment only — never folded into [indexUnrounded].
                    acceleration(mT, mTPrev)
                }
            }
        } else {
            mT = null
            mH = null
            g = null
            dP12 = null
            indexUnrounded = null
        }

        val available = indexUnrounded != null
        val reasons = if (available) {
            emptyList()
        } else {
            buildReasons(
                sample = sample,
                slopeWindow = slopeWindow,
                pressureWindow = pressureWindow,
            )
        }

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
                feelsLikeC = sample.feelsLikeC,
            ),
            // 12-hour net pressure change (field name retained for API stability).
            pressureTrend3hHpa = dP12?.let {
                BigDecimal.valueOf(it).setScale(SCALE_DELTA, RoundingMode.HALF_UP)
            },
            // Feel / diagnostics drivers: pressure=dP12, humidity=mH, temperature=mT.
            // Gap slope g stays internal to the score only.
            subScores = SubScores(
                pressure = dP12?.let {
                    BigDecimal.valueOf(it).setScale(SCALE_DELTA, RoundingMode.HALF_UP)
                },
                humidity = mH?.let {
                    BigDecimal.valueOf(it).setScale(SCALE_DELTA, RoundingMode.HALF_UP)
                },
                temperature = mT?.let {
                    BigDecimal.valueOf(it).setScale(SCALE_DELTA, RoundingMode.HALF_UP)
                },
            ),
            indexRaw = indexUnrounded?.let {
                BigDecimal.valueOf(it).setScale(SCALE_INDEX_RAW, RoundingMode.HALF_UP)
            },
            indexScore = indexUnrounded?.let {
                BigDecimal.valueOf(it).setScale(SCALE_INDEX_SCORE, RoundingMode.HALF_UP)
            },
            indexDisplay = indexUnrounded?.let {
                BigDecimal.valueOf(it).setScale(0, RoundingMode.HALF_UP).intValueExact()
            },
            status = if (available) HourStatus.AVAILABLE else HourStatus.UNAVAILABLE,
            unavailableReasons = reasons,
        )
    }

    /**
     * Contiguous hourly samples ending at [endEpoch], oldest first.
     * Null when any epoch is missing from [byEpoch].
     */
    private fun window(
        endEpoch: Long,
        count: Int,
        byEpoch: Map<Long, HourlyObservation>,
    ): List<HourlyObservation>? {
        val samples = ArrayList<HourlyObservation>(count)
        for (i in (count - 1) downTo 0) {
            val sample = byEpoch[endEpoch - i * HOUR_SECONDS] ?: return null
            samples += sample
        }
        return samples
    }

    private fun buildReasons(
        sample: HourlyObservation,
        slopeWindow: List<HourlyObservation>?,
        pressureWindow: List<HourlyObservation>?,
    ): List<String> =
        buildList {
            if (slopeWindow == null) {
                add(REASON_SLOPE_WINDOW)
            } else {
                slopeWindow.forEach { hour ->
                    if (hour.temperatureC == null) add(REASON_TEMPERATURE_NULL)
                    if (hour.relativeHumidityPct == null) add(REASON_HUMIDITY_NULL)
                }
            }
            if (pressureWindow == null) {
                add(REASON_PRESSURE_WINDOW)
            } else {
                // Only endpoints feed dP12; middle nulls do not block scoring.
                if (pressureWindow.first().surfacePressureHpa == null) {
                    add(REASON_PRESSURE_NULL)
                }
                if (pressureWindow.last().surfacePressureHpa == null) {
                    add(REASON_PRESSURE_NULL)
                }
            }
            if (sample.surfacePressureHpa == null) add(REASON_PRESSURE_NULL)
            if (sample.relativeHumidityPct == null) add(REASON_HUMIDITY_NULL)
            if (sample.temperatureC == null) add(REASON_TEMPERATURE_NULL)
        }.distinct()

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
}
