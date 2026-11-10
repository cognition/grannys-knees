package ca.aeo3.grannysknees.data

import java.time.Instant

/**
 * On-disk cache envelope for a single Open-Meteo forecast response.
 *
 * Stores the raw JSON body so [ca.aeo3.grannysknees.weather.OpenMeteoParser]
 * remains the single parser. A corrupt or unreadable envelope is treated as
 * empty cache — never fabricated weather (contract §6 / §7).
 */
internal data class ForecastCacheEnvelope(
    val fetchedAt: Instant,
    val locationLabel: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val rawBody: String,
)
