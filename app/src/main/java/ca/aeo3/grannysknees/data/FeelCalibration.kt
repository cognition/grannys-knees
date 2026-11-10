package ca.aeo3.grannysknees.data

import java.time.Duration
import java.time.Instant
import kotlin.math.sqrt

/**
 * Pure Feel→Personal calibration: collapse, consecutive counting, suggest, blend.
 */
internal object FeelCalibration {

    /** Saves within this window share one calibration time-point. */
    val COLLAPSE_WINDOW: Duration = Duration.ofMinutes(15)

    /** Minimum gap between consecutive counted time-points. */
    val MIN_CONSECUTIVE_GAP: Duration = Duration.ofMinutes(45)

    /** Counted consecutive usable points required before Feel may tune. */
    const val UNLOCK_COUNT: Int = 10

    /** Weight of the Feel suggestion when blending onto the Adj/DEFAULT base. */
    const val BLEND_ALPHA: Float = 0.25f

    /**
     * Scales residual×driver averages onto a sensitivity multiplier around 1.
     */
    const val SUGGEST_GAIN: Double = 0.08

    private const val DRIVER_NOISE: Double = 0.05

    /**
     * Mean and sample standard deviation of [ratings].
     *
     * One value yields stddev 0. Empty list is invalid.
     */
    fun painStats(ratings: List<Int>): Pair<Double, Double> {
        require(ratings.isNotEmpty()) { "reported ratings required" }
        val mean = ratings.map { it.toDouble() }.average()
        if (ratings.size == 1) {
            return mean to 0.0
        }
        val variance = ratings.sumOf { value ->
            val delta = value.toDouble() - mean
            delta * delta
        } / (ratings.size - 1)
        return mean to sqrt(variance)
    }

    /**
     * Upserts [incoming] into [existing] using the 15-minute collapse rule.
     *
     * Matching samples union reported ratings and keep the later centre / weather.
     */
    fun upsertSample(
        existing: List<FeelCalibrationSample>,
        incoming: FeelCalibrationSample,
    ): List<FeelCalibrationSample> {
        require(incoming.ratings.isNotEmpty()) { "incoming ratings required" }
        val matchIndex = existing.indexOfLast { sample ->
            Duration.between(sample.centreAt, incoming.centreAt).abs() <= COLLAPSE_WINDOW
        }
        if (matchIndex < 0) {
            return (existing + incoming).sortedBy { it.centreAt }
        }
        val prior = existing[matchIndex]
        val merged = incoming.copy(
            ratings = prior.ratings + incoming.ratings,
            observationIds = (prior.observationIds + incoming.observationIds).distinct(),
            centreAt = maxOf(prior.centreAt, incoming.centreAt),
        )
        return existing.toMutableList().also { it[matchIndex] = merged }.sortedBy { it.centreAt }
    }

    /**
     * Counts consecutive usable samples where each counted step is ≥45 minutes
     * after the previous counted centre.
     */
    fun consecutiveCountable(samples: List<FeelCalibrationSample>): List<FeelCalibrationSample> {
        val usable = samples.filter { it.weatherWindowMean.isFinite() }
            .sortedBy { it.centreAt }
        if (usable.isEmpty()) return emptyList()
        val counted = mutableListOf(usable.first())
        for (sample in usable.drop(1)) {
            val gap = Duration.between(counted.last().centreAt, sample.centreAt)
            if (gap >= MIN_CONSECUTIVE_GAP) {
                counted += sample
            }
        }
        return counted
    }

    /** True when Feel may change coefficients. */
    fun isUnlocked(samples: List<FeelCalibrationSample>): Boolean =
        consecutiveCountable(samples).size >= UNLOCK_COUNT

    /**
     * Suggests sensitivity multipliers from consecutive counted samples.
     *
     * Residual = pain mean − weather window mean. Each factor is nudged when
     * its signed weather driver exceeds noise, weighted by inverse stddev.
     */
    fun suggest(samples: List<FeelCalibrationSample>): SensitivityProfile {
        val counted = consecutiveCountable(samples)
        if (counted.size < UNLOCK_COUNT) {
            return SensitivityProfile.DEFAULT
        }
        return SensitivityProfile(
            pressure = suggestFactor(counted) { sample ->
                sample.deltaPressure?.let { -it }
            },
            humidity = suggestFactor(counted) { sample ->
                sample.deltaHumidity
            },
            cold = suggestFactor(counted) { sample ->
                sample.deltaTemperature?.let { -it }
            },
        ).clamped()
    }

    /**
     * Blends [suggested] onto [base] with [BLEND_ALPHA].
     */
    fun blend(
        base: SensitivityProfile,
        suggested: SensitivityProfile,
        alpha: Float = BLEND_ALPHA,
    ): SensitivityProfile {
        val a = alpha.coerceIn(0f, 1f)
        val keep = 1f - a
        return SensitivityProfile(
            pressure = keep * base.pressure + a * suggested.pressure,
            humidity = keep * base.humidity + a * suggested.humidity,
            cold = keep * base.cold + a * suggested.cold,
        ).clamped()
    }

    private fun suggestFactor(
        samples: List<FeelCalibrationSample>,
        driverOf: (FeelCalibrationSample) -> Double?,
    ): Float {
        var weightedSum = 0.0
        var weightTotal = 0.0
        for (sample in samples) {
            val driver = driverOf(sample) ?: continue
            if (kotlin.math.abs(driver) < DRIVER_NOISE) continue
            val residual = sample.meanRating - sample.weatherWindowMean
            val weight = 1.0 / (1.0 + sample.ratingStdDev)
            weightedSum += weight * residual * kotlin.math.sign(driver)
            weightTotal += weight
        }
        if (weightTotal <= 0.0) {
            return SensitivityProfile.DEFAULT_SENSITIVITY
        }
        val mean = weightedSum / weightTotal
        val suggested = 1.0 + SUGGEST_GAIN * mean
        return suggested.toFloat().coerceIn(SensitivityProfile.MIN, SensitivityProfile.MAX)
    }
}
