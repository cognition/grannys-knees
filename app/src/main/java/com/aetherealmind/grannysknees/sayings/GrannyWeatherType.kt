package com.aetherealmind.grannysknees.sayings

import com.aetherealmind.grannysknees.index.HourIndex
import com.aetherealmind.grannysknees.index.HourStatus
import com.aetherealmind.grannysknees.weather.ForecastHour
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

/**
 * Labels used in granny-says.json `weatherTypes` arrays.
 *
 * A saying with one or more types is only eligible when the live forecast
 * activates at least one of those labels.
 */
internal object GrannyWeatherType {
    const val SNOW = "snow"
    const val RAIN = "rain"
    const val COLD = "cold"
    const val CLOUDY = "cloudy"
    const val SUNNY = "sunny"
    const val HUMID = "humid"
    const val WINDY = "windy"
    const val HOT = "hot"
    const val ICY = "icy"
    const val THUNDERSTORM = "thunderstorm"
    const val COLD_FRONT = "cold_front"
    const val PRESSURE_CHANGE = "pressure_change"

    private val SNOW_CODES = setOf(71, 73, 75, 77, 85, 86)
    private val THUNDERSTORM_CODES = setOf(95, 96, 99)
    private val ICY_CODES = setOf(56, 57, 66, 67)
    private val RAIN_CODES = setOf(
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99,
    )
    private val CLOUDY_CODES = setOf(1, 2, 3, 45, 48)
    private val SUNNY_CODES = setOf(0)

    /** Absolute |dP12| (hPa) that counts as a noticeable pressure shift. */
    private const val PRESSURE_CHANGE_HPA = 1.0

    /** Relative humidity (%) treated as humid for sayings. */
    private const val HUMID_THRESHOLD = 75

    /** Air temperature (°C) at or below this counts as cold. */
    private const val COLD_THRESHOLD_C = 0.0

    /**
     * Air or apparent temperature (°C) at or above this counts as hot
     * ("bloody hot" / fan weather).
     */
    private const val HOT_THRESHOLD_C = 28.0

    /**
     * Wind speed at 10 m (km/h) at or above this counts as windy
     * (bin-down-the-street weather).
     */
    private const val WINDY_THRESHOLD_KMH = 30.0

    /**
     * Cooling (°C) within [COLD_FRONT_MAX_SPAN_SECONDS] that counts as a cold
     * front arriving. Ordinary evening cooling over a longer window does not.
     */
    private const val COLD_FRONT_DROP_C = 3.0

    /** Max span for a cold-front drop (3 hours). */
    private const val COLD_FRONT_MAX_SPAN_SECONDS = 3L * 3600L

    /**
     * Active weather labels near [referenceNow] from the hourly forecast and
     * scored index (for pressure change).
     */
    fun activeTypes(
        hours: List<ForecastHour>,
        scoredHours: List<HourIndex>,
        zoneId: ZoneId,
        referenceNow: Instant,
    ): Set<String> {
        if (hours.isEmpty()) return emptySet()
        val nowEpoch = referenceNow.epochSecond
        val nearby = hours.filter { hour ->
            abs(hour.epoch - nowEpoch) <= 6L * 3600L
        }.ifEmpty { hours }
        val active = linkedSetOf<String>()
        nearby.forEach { hour ->
            when (val code = hour.weatherCode) {
                in SNOW_CODES -> active += SNOW
                in RAIN_CODES -> active += RAIN
                in CLOUDY_CODES -> active += CLOUDY
                in SUNNY_CODES -> active += SUNNY
            }
            if (hour.weatherCode in THUNDERSTORM_CODES) {
                active += THUNDERSTORM
            }
            if (hour.weatherCode in ICY_CODES) {
                active += ICY
            }
            hour.temperature2m?.takeIf { it <= COLD_THRESHOLD_C }?.let {
                active += COLD
            }
            val feelHot = listOfNotNull(hour.temperature2m, hour.apparentTemperature)
                .any { it >= HOT_THRESHOLD_C }
            if (feelHot) {
                active += HOT
            }
            hour.relativeHumidity2m?.takeIf { it >= HUMID_THRESHOLD }?.let {
                active += HUMID
            }
            hour.windSpeed10m?.takeIf { it >= WINDY_THRESHOLD_KMH }?.let {
                active += WINDY
            }
        }
        if (isColdFront(nearby)) {
            active += COLD_FRONT
        }
        val hourStart = referenceNow.atZone(zoneId)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .toEpochSecond()
        val currentScored = scoredHours.firstOrNull { hour ->
            hour.epoch == hourStart && hour.status == HourStatus.AVAILABLE
        } ?: scoredHours.firstOrNull { hour ->
            hour.status == HourStatus.AVAILABLE && abs(hour.epoch - hourStart) <= 3600L
        }
        val pressureDelta = currentScored?.subScores?.pressure?.toDouble()
            ?: currentScored?.pressureTrend3hHpa?.toDouble()
        if (pressureDelta != null && abs(pressureDelta) >= PRESSURE_CHANGE_HPA) {
            active += PRESSURE_CHANGE
        }
        return active
    }

    /**
     * True when nearby hours cool by at least [COLD_FRONT_DROP_C] within
     * [COLD_FRONT_MAX_SPAN_SECONDS]. Earliest→latest over the full ±6 h window
     * is too eager for calm evening cooling.
     */
    private fun isColdFront(nearby: List<ForecastHour>): Boolean {
        val temps = nearby
            .sortedBy { it.epoch }
            .mapNotNull { hour -> hour.temperature2m?.let { hour.epoch to it } }
        if (temps.size < 2) return false
        for (i in temps.indices) {
            val (startEpoch, startTemp) = temps[i]
            for (j in i + 1 until temps.size) {
                val (endEpoch, endTemp) = temps[j]
                if (endEpoch - startEpoch > COLD_FRONT_MAX_SPAN_SECONDS) break
                if (startTemp - endTemp >= COLD_FRONT_DROP_C) return true
            }
        }
        return false
    }
}
