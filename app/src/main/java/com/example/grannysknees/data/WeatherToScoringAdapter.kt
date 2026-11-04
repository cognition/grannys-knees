package com.example.grannysknees.data

import com.example.grannysknees.index.HourlyObservation
import com.example.grannysknees.index.ScoringRequest
import com.example.grannysknees.weather.HourlyForecast

/**
 * Maps a parsed Open-Meteo [HourlyForecast] into a [ScoringRequest] for index-v1.
 *
 * Preserves nulls — never substitutes missing weather with zero (contract §7).
 */
internal object WeatherToScoringAdapter {
    fun toScoringRequest(forecast: HourlyForecast): ScoringRequest =
        ScoringRequest(
            zoneId = forecast.timezone.zone.id,
            hours = forecast.hours.map { hour ->
                HourlyObservation(
                    epoch = hour.epoch,
                    surfacePressureHpa = hour.surfacePressure,
                    relativeHumidityPct = hour.relativeHumidity2m?.toDouble(),
                    temperatureC = hour.temperature2m,
                )
            },
        )
}
