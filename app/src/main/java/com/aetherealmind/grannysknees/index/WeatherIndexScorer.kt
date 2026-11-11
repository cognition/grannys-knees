package com.aetherealmind.grannysknees.index

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Pure-Kotlin implementation of the experimental **12-hour** weather index
 * (`docs/contracts/weather-index-v2.md`).
 *
 * Each scored hour is compared to the observation exactly twelve elapsed hours
 * earlier (UTC epoch match). Falling pressure, falling temperature, and rising
 * humidity raise the signal. The result is a playful 1–10 heuristic — **not
 * medical advice**.
 *
 * Arithmetic uses [BigDecimal] with half-up quantisation so fixture equality
 * stays exact on canonical scales.
 */
object WeatherIndexScorer {

    const val CONTRACT_VERSION: String = "weather-index/2.0.0"
    const val INDEX_VERSION: String = "index-v2"

    /** Reported tie-break rule for daily low/high (contract §8.4). */
    const val TIE_BREAK: String = "earliest epoch"

    private const val WINDOW_SECONDS: Long = 12L * 3_600L
    private const val COMPLETE_DAY_MIN_AVAILABLE_HOURS = 18
    private const val PERSONAL_INDEX_VERSION: String = "index-v2-personal"

    /** Historical raw-signal floor that maps to index 1 (Ottawa ERA5 calibration). */
    private val SIGNAL_FLOOR = BigDecimal("-89.4")

    /** Historical raw-signal span from floor to ceiling (89.4 + 88.4). */
    private val SIGNAL_SPAN = BigDecimal("177.8")

    private val INDEX_MIN = BigDecimal.ONE
    private val INDEX_MAX = BigDecimal.TEN
    private val NINE = BigDecimal("9")
    private val TWO = BigDecimal("2")

    private const val SCALE_DELTA = 2
    private const val SCALE_INDEX_RAW = 4
    private const val SCALE_INDEX_SCORE = 1

    private const val REASON_PRESSURE_NULL = "surface_pressure null"
    private const val REASON_HUMIDITY_NULL = "relative_humidity_2m null"
    private const val REASON_TEMPERATURE_NULL = "temperature_2m null"
    private const val REASON_WINDOW =
        "12-hour window unavailable (no comparable t-12h sample)"

    private val MC = MathContext.DECIMAL128

    private val LOCAL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")
    private val UTC_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

    /** Score every hour with baseline (neutral) sensitivities. */
    fun score(request: ScoringRequest): IndexResult =
        score(request, DeltaSensitivities.BASELINE)

    /**
     * Score every hour, optionally applying personal **influence** weights to
     * each signed contribution before the signal map.
     *
     * Influence means how much that weather factor affects the person — not
     * “high vs low pressure” or “hot vs cold”. Higher influence amplifies
     * contributions that raise discomfort and dampens relief from that factor,
     * so dialling influence up does not push Personal *down* on a calming day.
     * Non-baseline sensitivities stamp [PERSONAL_INDEX_VERSION].
     */
    internal fun score(
        request: ScoringRequest,
        sensitivities: DeltaSensitivities,
    ): IndexResult {
        val zone = ZoneId.of(request.zoneId)
        val byEpoch = request.hours.associateBy { it.epoch }
        val hours = request.hours.map { sample ->
            scoreHour(sample, byEpoch, zone, sensitivities)
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

    private fun scoreHour(
        sample: HourlyObservation,
        byEpoch: Map<Long, HourlyObservation>,
        zone: ZoneId,
        sensitivities: DeltaSensitivities,
    ): HourIndex {
        val instant = Instant.ofEpochSecond(sample.epoch)
        val zoned = instant.atZone(zone)
        val prior = byEpoch[sample.epoch - WINDOW_SECONDS]

        val pressure = sample.surfacePressureHpa?.let { BigDecimal.valueOf(it) }
        val humidity = sample.relativeHumidityPct?.let { BigDecimal.valueOf(it) }
        val temperature = sample.temperatureC?.let { BigDecimal.valueOf(it) }
        val priorPressure = prior?.surfacePressureHpa?.let { BigDecimal.valueOf(it) }
        val priorHumidity = prior?.relativeHumidityPct?.let { BigDecimal.valueOf(it) }
        val priorTemperature = prior?.temperatureC?.let { BigDecimal.valueOf(it) }

        val deltaPressure =
            if (pressure != null && priorPressure != null) pressure - priorPressure else null
        val deltaHumidity =
            if (humidity != null && priorHumidity != null) humidity - priorHumidity else null
        val deltaTemperature =
            if (temperature != null && priorTemperature != null) {
                temperature - priorTemperature
            } else {
                null
            }

        val signal: BigDecimal? =
            if (deltaPressure != null && deltaHumidity != null && deltaTemperature != null) {
                // Contributions match baseline signal = ΔH − ΔP − ΔT.
                val humidityContribution = scaleContribution(deltaHumidity, sensitivities.humidity)
                val pressureContribution =
                    scaleContribution(deltaPressure.negate(), sensitivities.pressure)
                val temperatureContribution =
                    scaleContribution(deltaTemperature.negate(), sensitivities.temperature)
                humidityContribution
                    .add(pressureContribution, MC)
                    .add(temperatureContribution, MC)
            } else {
                null
            }

        val indexUnrounded: BigDecimal? = signal?.let { mapSignal(it) }
        val available = indexUnrounded != null
        val reasons = if (available) emptyList() else buildReasons(sample, prior)

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
            // 12-hour pressure change (field name retained for API stability).
            pressureTrend3hHpa = deltaPressure?.setScale(SCALE_DELTA, RoundingMode.HALF_UP),
            subScores = SubScores(
                pressure = deltaPressure?.setScale(SCALE_DELTA, RoundingMode.HALF_UP),
                humidity = deltaHumidity?.setScale(SCALE_DELTA, RoundingMode.HALF_UP),
                temperature = deltaTemperature?.setScale(SCALE_DELTA, RoundingMode.HALF_UP),
            ),
            indexRaw = indexUnrounded?.setScale(SCALE_INDEX_RAW, RoundingMode.HALF_UP),
            indexScore = indexUnrounded?.setScale(SCALE_INDEX_SCORE, RoundingMode.HALF_UP),
            indexDisplay = indexUnrounded?.setScale(0, RoundingMode.HALF_UP)?.intValueExact(),
            status = if (available) HourStatus.AVAILABLE else HourStatus.UNAVAILABLE,
            unavailableReasons = reasons,
        )
    }

    /**
     * Maps a raw signal onto 1–10 using the fixed Ottawa ERA5 anchors, then
     * clamps. Rounding is applied by the caller from this unrounded value.
     */
    internal fun mapSignal(signal: BigDecimal): BigDecimal {
        val shifted = signal.subtract(SIGNAL_FLOOR, MC)
        val scaled = shifted.multiply(NINE, MC).divide(SIGNAL_SPAN, MC)
        return INDEX_MIN.add(scaled, MC).coerceIn(INDEX_MIN, INDEX_MAX)
    }

    /**
     * Scales one signed contribution by an influence multiplier.
     *
     * Positive contributions (toward discomfort) are multiplied by [sensitivity].
     * Negative (relief) contributions are multiplied by `2 − sensitivity`, so
     * at the default `1` nothing changes, at strong influence (`2`) relief from
     * that factor is ignored, and at `0` that factor does not push discomfort.
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

    private fun buildReasons(
        sample: HourlyObservation,
        prior: HourlyObservation?,
    ): List<String> =
        buildList {
            if (sample.surfacePressureHpa == null) add(REASON_PRESSURE_NULL)
            if (sample.relativeHumidityPct == null) add(REASON_HUMIDITY_NULL)
            if (sample.temperatureC == null) add(REASON_TEMPERATURE_NULL)
            if (prior == null) {
                add(REASON_WINDOW)
            } else {
                if (prior.surfacePressureHpa == null) add(REASON_PRESSURE_NULL)
                if (prior.relativeHumidityPct == null) add(REASON_HUMIDITY_NULL)
                if (prior.temperatureC == null) add(REASON_TEMPERATURE_NULL)
            }
        }

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
