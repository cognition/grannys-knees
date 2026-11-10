package ca.aeo3.grannysknees.data

import ca.aeo3.grannysknees.index.HourIndex
import ca.aeo3.grannysknees.index.HourStatus
import java.time.Duration
import java.time.Instant

/**
 * Builds a [FeelWeatherCapture] from scored Standard hours around a Feel save.
 */
internal object FeelWeatherWindow {

    /** Hours included on each side of the report time. */
    val HALF_WINDOW: Duration = Duration.ofHours(3)

    /**
     * Mean AVAILABLE index in [T−3h, T+3h] plus nearest-hour 12h deltas.
     */
    fun capture(
        hours: List<HourIndex>,
        at: Instant,
    ): FeelWeatherCapture {
        val centreEpoch = at.epochSecond
        val start = centreEpoch - HALF_WINDOW.seconds
        val end = centreEpoch + HALF_WINDOW.seconds
        val inWindow = hours.filter { hour ->
            hour.status == HourStatus.AVAILABLE &&
                hour.indexScore != null &&
                hour.epoch in start..end
        }
        val windowMean = if (inWindow.isEmpty()) {
            null
        } else {
            inWindow.map { it.indexScore!!.toDouble() }.average()
        }
        val nearest = hours
            .filter { it.status == HourStatus.AVAILABLE }
            .minByOrNull { hour -> kotlin.math.abs(hour.epoch - centreEpoch) }
        return FeelWeatherCapture(
            weatherWindowMean = windowMean,
            deltaPressure = nearest?.subScores?.pressure?.toDouble(),
            deltaHumidity = nearest?.subScores?.humidity?.toDouble(),
            deltaTemperature = nearest?.subScores?.temperature?.toDouble(),
        )
    }
}
