package ca.aeo3.grannysknees.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Geocoding URL and row rules, without a device. */
class OpenMeteoGeocodingClientTest {
    private val client = OpenMeteoGeocodingClient(transport = HttpTransport { "" })

    @Test
    fun `search url limits results to Canada`() {
        val url = client.buildUrl("London")

        assertTrue(url.contains("countryCode=CA"))
        assertTrue(url.contains("name=London"))
    }

    @Test
    fun `london ontario is kept and a row without a timezone is dropped`() {
        val body = """
            {"results":[
              {"name":"London","latitude":42.98,"longitude":-81.23,"timezone":"America/Toronto","country_code":"CA","admin1":"Ontario"},
              {"name":"Incomplete","latitude":1.0,"longitude":2.0,"country_code":"CA","admin1":"Quebec"}
            ]}
        """.trimIndent()

        val cities = client.parse(body)

        assertEquals(1, cities.size)
        assertEquals("London, Ontario", cities.single().label)
        assertEquals("America/Toronto", cities.single().timezone)
    }

    @Test
    fun `a body without results is an empty list`() {
        assertTrue(client.parse("""{"generationtime_ms":0.1}""").isEmpty())
    }
}
