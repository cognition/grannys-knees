package com.example.grannysknees.weather

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * The hourly forecast variables the weather index requires, in the canonical
 * Open-Meteo spelling.
 */
private val REQUIRED_HOURLY_VARIABLES = listOf(
    "temperature_2m",
    "relative_humidity_2m",
    "surface_pressure",
)

/**
 * Hours before "now" so the **whole local calendar day** is in the payload,
 * plus the 12-hour trailing window lead-in before local midnight (`index-v2`).
 *
 * With only twelve past hours, a late-evening open would leave the morning
 * blank on Today's curve.
 */
const val DEFAULT_PAST_HOURS: Int = 24 + 12

/**
 * A single Open-Meteo `/v1/forecast` request.
 *
 * Defaults: `timeformat=unixtime`, `forecast_days >= 6` (5-day outlook +
 * current local day), and `past_hours >= 12` for the 12-hour index window.
 * [DEFAULT_PAST_HOURS] also covers morning→night on Today's curve.
 */
data class OpenMeteoRequest(
    val latitude: Double,
    val longitude: Double,
    /** IANA timezone name, e.g. `America/Toronto` (contract §4). */
    val timezone: String,
    val hourlyVariables: List<String> = REQUIRED_HOURLY_VARIABLES,
    val forecastDays: Int = 6,
    val pastHours: Int = DEFAULT_PAST_HOURS,
) {
    init {
        require(forecastDays >= 6) { "forecast_days must be >= 6 (contract §2.1)" }
        require(pastHours >= 12) {
            "past_hours must be >= 12 for the 12-hour index window"
        }
        require(hourlyVariables.isNotEmpty()) { "at least one hourly variable is required" }
    }
}

/** Outcome of a forecast fetch. Network and parse failures are distinct and never faked. */
sealed interface ForecastResult {
    data class Success(val forecast: HourlyForecast) : ForecastResult

    /** Transport-level failure (no/invalid HTTP response). */
    data class NetworkError(val cause: Throwable) : ForecastResult

    /** A response arrived but could not be parsed into the contract model. */
    data class ParseError(val cause: OpenMeteoParseException) : ForecastResult
}

/**
 * Minimal HTTP transport so the client can be driven by a fake in tests without
 * touching the network. Implementations must throw [IOException] for any
 * transport-level failure (connection refused, timeout, non-2xx status).
 */
fun interface HttpTransport {
    @Throws(IOException::class)
    fun get(url: String): String
}

/** Default [HttpTransport] backed by [HttpURLConnection]; no third-party HTTP dependency. */
class UrlHttpTransport(
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 10_000,
) : HttpTransport {

    @Throws(IOException::class)
    override fun get(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/json")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("Open-Meteo returned HTTP $status ${connection.responseMessage}")
            }
            return connection.inputStream.bufferedReader(StandardCharsets.UTF_8)
                .use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * Open-Meteo hourly forecast client. Builds a contract-compliant request URL,
 * fetches it over the injected [transport], and parses the response into
 * [HourlyForecast] with DST-correct IANA timestamps.
 *
 * Errors are reported, never fabricated: a transport failure yields
 * [ForecastResult.NetworkError] and a bad payload yields
 * [ForecastResult.ParseError]; neither returns zero-filled weather data.
 */
class OpenMeteoClient(
    private val transport: HttpTransport = UrlHttpTransport(),
    private val baseUrl: String = DEFAULT_BASE_URL,
) {

    fun fetchForecast(request: OpenMeteoRequest): ForecastResult {
        val url = buildUrl(request)
        val body = try {
            transport.get(url)
        } catch (e: IOException) {
            return ForecastResult.NetworkError(e)
        }
        return try {
            ForecastResult.Success(OpenMeteoParser.parse(body))
        } catch (e: OpenMeteoParseException) {
            ForecastResult.ParseError(e)
        }
    }

    /** Builds the `/v1/forecast` URL with the contract-required query parameters (§2.1). */
    fun buildUrl(request: OpenMeteoRequest): String {
        val params = buildList {
            add("latitude" to formatCoordinate(request.latitude))
            add("longitude" to formatCoordinate(request.longitude))
            add("hourly" to request.hourlyVariables.joinToString(","))
            add("timezone" to request.timezone)
            add("timeformat" to TIMEFORMAT_UNIXTIME)
            add("forecast_days" to request.forecastDays.toString())
            add("past_hours" to request.pastHours.toString())
        }
        val query = params.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return "$baseUrl?$query"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    /** Avoids a locale-dependent decimal separator in the coordinate. */
    private fun formatCoordinate(value: Double): String = value.toString()

    companion object {
        const val DEFAULT_BASE_URL = "https://api.open-meteo.com/v1/forecast"
        const val TIMEFORMAT_UNIXTIME = "unixtime"
    }
}
