package com.example.grannysknees.data

import com.example.grannysknees.index.HourlyObservation
import com.example.grannysknees.index.ScoringRequest
import com.example.grannysknees.weather.HourlyForecast
import java.time.Instant

/**
 * Maps a parsed Open-Meteo [HourlyForecast] into a [ScoringRequest] for index-v1.
 *
 * Preserves nulls — never substitutes missing weather with zero (contract §7).
 */
internal object WeatherToScoringAdapter {
    fun toScoringRequest(
        forecast: HourlyForecast,
        localPressureHpa: Double? = null,
        referenceNow: Instant? = null,
    ): ScoringRequest {
        val localHour = if (localPressureHpa != null && referenceNow != null) {
            forecast.hours.minByOrNull { kotlin.math.abs(it.epoch - referenceNow.epochSecond) }
        } else {
            null
        }
        return ScoringRequest(
            zoneId = forecast.timezone.zone.id,
            hours = forecast.hours.map { hour ->
                HourlyObservation(
                    epoch = hour.epoch,
                    surfacePressureHpa = if (hour == localHour) localPressureHpa
                    else hour.surfacePressure,
                    relativeHumidityPct = hour.relativeHumidity2m?.toDouble(),
                    temperatureC = hour.temperature2m,
                )
            },
        )
    }
}
