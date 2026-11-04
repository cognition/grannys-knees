package com.example.grannysknees.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.OffsetDateTime

/**
 * Parser tests for the T03 Open-Meteo client. They validate the DST-correct
 * IANA conversion and null handling against the canonical contract fixtures
 * (`docs/contracts/fixtures/`). Only T03-owned fields are asserted — epoch,
 * instant, local time, per-row offset, local date, and the raw provider inputs.
 * Index scores and sub-scores are the scorer's concern (T04) and are not checked.
 */
class OpenMeteoParserTest {

    /**
     * For each listed fixture, every parsed hour must match the expected
     * instant/offset/local fields and raw inputs, field for field. This is the
     * strongest evidence that the client reproduces the contract fixtures.
     */
    @Test
    fun `parsed hours match contract fixtures field for field`() {
        val cases = listOf(
            "nominal",
            "missing-values",
            "pressure-trend-boundary",
            "dst-fall-back",
            "dst-spring-forward",
        )
        for (case in cases) {
            val forecast = OpenMeteoParser.parse(FixtureSupport.inputBody(case))
            val expected = FixtureSupport.expectedHours(case)
            assertEquals("row count for $case", expected.size, forecast.hours.size)

            forecast.hours.forEachIndexed { i, hour ->
                val e = expected[i]
                val where = "$case[$i]"
                assertEquals("$where epoch", e.epoch, hour.epoch)
                assertEquals("$where instantUtc", Instant.parse(e.instantUtc), hour.instant)
                assertEquals(
                    "$where local",
                    OffsetDateTime.parse(e.local),
                    hour.local.toOffsetDateTime(),
                )
                assertEquals("$where utcOffsetSeconds", e.utcOffsetSeconds, hour.utcOffsetSeconds)
                assertEquals("$where localDate", e.localDate, hour.localDate.toString())
                assertEquals("$where surface_pressure", e.surfacePressure, hour.surfacePressure)
                assertEquals("$where relative_humidity_2m", e.relativeHumidity2m, hour.relativeHumidity2m)
                assertEquals("$where temperature_2m", e.temperature2m, hour.temperature2m)
            }
        }
    }

    @Test
    fun `nominal fixture parses a clean EST winter day`() {
        val forecast = OpenMeteoParser.parse(FixtureSupport.inputBody("nominal"))

        assertEquals(27, forecast.hours.size)
        assertEquals("America/Toronto", forecast.timezone.zone.id)
        assertEquals(-18000, forecast.timezone.utcOffsetSecondsAtRequest)
        assertEquals("°C", forecast.units.temperature2m)
        assertEquals("%", forecast.units.relativeHumidity2m)
        assertEquals("hPa", forecast.units.surfacePressure)
        assertEquals("unixtime", forecast.units.time)

        // Every hour has all three inputs present (no gaps in the nominal case).
        assertTrue(forecast.hours.all { it.temperature2m != null })
        assertTrue(forecast.hours.all { it.relativeHumidity2m != null })
        assertTrue(forecast.hours.all { it.surfacePressure != null })

        val first = forecast.hours.first()
        assertEquals(1794276000L, first.epoch)
        assertEquals(-18000, first.utcOffsetSeconds)
    }

    @Test
    fun `fall-back day keeps 25 local hours with the 0100 label repeated at two offsets`() {
        val forecast = OpenMeteoParser.parse(FixtureSupport.inputBody("dst-fall-back"))

        // 3 lead-in + 25 local hours for 2026-11-01 (America/Toronto).
        assertEquals(28, forecast.hours.size)

        val fallBackDay = forecast.hours.filter { it.localDate.toString() == "2026-11-01" }
        assertEquals("fall-back local day has 25 hours", 25, fallBackDay.size)

        // The 01:00 wall label occurs twice, on different instants and offsets:
        // first EDT (-04:00) then EST (-05:00).
        val oneAmRows = fallBackDay.filter { it.local.toLocalTime().toString() == "01:00" }
        assertEquals("01:00 occurs twice", 2, oneAmRows.size)
        assertEquals(-14400, oneAmRows[0].utcOffsetSeconds)
        assertEquals(-18000, oneAmRows[1].utcOffsetSeconds)
        assertTrue("the two 01:00 rows are distinct instants", oneAmRows[0].epoch < oneAmRows[1].epoch)
    }

    @Test
    fun `spring-forward day keeps 23 local hours with the 0200 wall hour absent`() {
        val forecast = OpenMeteoParser.parse(FixtureSupport.inputBody("dst-spring-forward"))

        // 3 lead-in + 23 local hours for 2026-03-08 (America/Toronto).
        assertEquals(26, forecast.hours.size)

        val springDay = forecast.hours.filter { it.localDate.toString() == "2026-03-08" }
        assertEquals("spring-forward local day has 23 hours", 23, springDay.size)
        assertTrue(
            "the 02:00 wall hour never occurs",
            springDay.none { it.local.toLocalTime().hour == 2 },
        )
    }

    @Test
    fun `missing values are preserved as null never zero`() {
        val forecast = OpenMeteoParser.parse(FixtureSupport.inputBody("missing-values"))

        fun hourAtLocal(local: String) =
            forecast.hours.first { it.local.toOffsetDateTime() == OffsetDateTime.parse(local) }

        // Null surface pressure at local 06:00–08:00 stays null (not 0.0).
        for (local in listOf(
            "2026-11-10T06:00:00-05:00",
            "2026-11-10T07:00:00-05:00",
            "2026-11-10T08:00:00-05:00",
        )) {
            val hour = hourAtLocal(local)
            assertNull("pressure at $local", hour.surfacePressure)
            // Other inputs on those rows remain present.
            assertTrue(hour.temperature2m != null)
            assertTrue(hour.relativeHumidity2m != null)
        }

        // Null humidity at local 12:00 stays null.
        val noon = hourAtLocal("2026-11-10T12:00:00-05:00")
        assertNull("humidity at noon", noon.relativeHumidity2m)
        assertTrue(noon.surfacePressure != null)
    }

    @Test
    fun `mismatched units are rejected`() {
        val fahrenheit = FixtureSupport.inputBody("nominal").replace("\"°C\"", "\"°F\"")
        val ex = assertThrows(OpenMeteoParseException::class.java) {
            OpenMeteoParser.parse(fahrenheit)
        }
        assertTrue(ex.message!!.contains("temperature_2m"))
    }

    @Test
    fun `a missing required variable is a parse error`() {
        val withoutPressure = FixtureSupport.inputBody("nominal")
            .replace("\"surface_pressure\"", "\"surface_pressure_removed\"")
        assertThrows(OpenMeteoParseException::class.java) {
            OpenMeteoParser.parse(withoutPressure)
        }
    }

    @Test
    fun `malformed json is a parse error`() {
        assertThrows(OpenMeteoParseException::class.java) {
            OpenMeteoParser.parse("{ not valid json")
        }
    }
}
