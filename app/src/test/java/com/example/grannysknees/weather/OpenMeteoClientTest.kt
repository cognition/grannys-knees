package com.example.grannysknees.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Client tests: URL construction against the contract's required parameters and
 * the three fetch outcomes (success / network error / parse error) driven by a
 * fake transport, so no real network call is made.
 */
class OpenMeteoClientTest {

    private val torontoRequest = OpenMeteoRequest(
        latitude = 43.65,
        longitude = -79.38,
        timezone = "America/Toronto",
    )

    private fun queryParams(url: String): Map<String, String> {
        val query = url.substringAfter('?')
        return query.split('&').associate { pair ->
            val (k, v) = pair.split('=', limit = 2)
            URLDecoder.decode(k, StandardCharsets.UTF_8.name()) to
                URLDecoder.decode(v, StandardCharsets.UTF_8.name())
        }
    }

    @Test
    fun `buildUrl carries the contract-required parameters`() {
        val url = OpenMeteoClient().buildUrl(torontoRequest)
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?"))

        val params = queryParams(url)
        assertEquals("43.65", params["latitude"])
        assertEquals("-79.38", params["longitude"])
        assertEquals("temperature_2m,relative_humidity_2m,surface_pressure", params["hourly"])
        assertEquals("America/Toronto", params["timezone"])
        assertEquals("unixtime", params["timeformat"])
    }

    @Test
    fun `buildUrl satisfies the forecast-days and lead-in minimums`() {
        val params = queryParams(OpenMeteoClient().buildUrl(torontoRequest))
        assertTrue("forecast_days >= 6", params["forecast_days"]!!.toInt() >= 6)
        assertTrue("past_hours >= 12", params["past_hours"]!!.toInt() >= 12)
    }

    @Test
    fun `default past hours cover a full local day plus lead-in`() {
        val params = queryParams(OpenMeteoClient().buildUrl(torontoRequest))
        assertEquals(DEFAULT_PAST_HOURS, params["past_hours"]!!.toInt())
        assertTrue(
            "past_hours covers 24h day + 12h lead-in",
            params["past_hours"]!!.toInt() >= 36,
        )
    }

    @Test
    fun `request rejects an insufficient lead-in`() {
        assertThrows(IllegalArgumentException::class.java) {
            OpenMeteoRequest(latitude = 1.0, longitude = 2.0, timezone = "UTC", pastHours = 11)
        }
    }

    @Test
    fun `request rejects fewer than six forecast days`() {
        assertThrows(IllegalArgumentException::class.java) {
            OpenMeteoRequest(latitude = 1.0, longitude = 2.0, timezone = "UTC", forecastDays = 5)
        }
    }

    @Test
    fun `fetch returns Success on a well-formed response`() {
        val transport = HttpTransport { FixtureSupport.inputBody("nominal") }
        val result = OpenMeteoClient(transport).fetchForecast(torontoRequest)

        assertTrue(result is ForecastResult.Success)
        val forecast = (result as ForecastResult.Success).forecast
        assertEquals(27, forecast.hours.size)
        assertEquals("America/Toronto", forecast.timezone.zone.id)
    }

    @Test
    fun `fetch returns NetworkError when the transport fails`() {
        val boom = IOException("connection refused")
        val transport = HttpTransport { throw boom }
        val result = OpenMeteoClient(transport).fetchForecast(torontoRequest)

        assertTrue(result is ForecastResult.NetworkError)
        assertSame(boom, (result as ForecastResult.NetworkError).cause)
    }

    @Test
    fun `fetch returns ParseError on a malformed response`() {
        val transport = HttpTransport { "{ not valid json" }
        val result = OpenMeteoClient(transport).fetchForecast(torontoRequest)

        assertTrue(result is ForecastResult.ParseError)
    }

    @Test
    fun `fetch never fabricates values when the transport fails`() {
        val transport = HttpTransport { throw IOException("offline") }
        val result = OpenMeteoClient(transport).fetchForecast(torontoRequest)
        // A transport failure must not degrade into an empty or zero-filled forecast.
        assertTrue(result !is ForecastResult.Success)
    }
}
