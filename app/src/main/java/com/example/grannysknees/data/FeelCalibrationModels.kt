package com.example.grannysknees.data

import java.time.Instant

/**
 * One usable Feel calibration time-point after 15-minute collapse.
 *
 * [meanRating] and [ratingStdDev] cover reported body parts only.
 * Weather fields come from the Standard index around [centreAt].
 */
internal data class FeelCalibrationSample(
    val centreAt: Instant,
    val ratings: List<Int>,
    val weatherWindowMean: Double,
    val deltaPressure: Double?,
    val deltaHumidity: Double?,
    val deltaTemperature: Double?,
    val observationIds: List<String> = emptyList(),
) {
    /** Mean of reported ratings. */
    val meanRating: Double
        get() = FeelCalibration.painStats(ratings).first

    /** Sample standard deviation of reported ratings. */
    val ratingStdDev: Double
        get() = FeelCalibration.painStats(ratings).second
}

/**
 * Weather context captured from the forecast cache for a Feel save.
 *
 * Null weather-window mean means the sample is not usable for calibration.
 */
internal data class FeelWeatherCapture(
    val weatherWindowMean: Double?,
    val deltaPressure: Double?,
    val deltaHumidity: Double?,
    val deltaTemperature: Double?,
)
