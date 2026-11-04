package com.example.grannysknees.index

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Fixture-backed tests for the experimental `index-v1` scorer.
 *
 * Each case drives [WeatherIndexScorer.score] with the Open-Meteo-shaped
 * `<case>.input.json` and asserts the result reproduces `<case>.expected.json`
 * exactly on the canonical quantised values (contract §9.4) — no floating-point
 * tolerance. Numbers are compared by value via [BigDecimal.compareTo] so scale
 * differences such as `5.1` vs `5.10` are equal while `5.1` vs `5.2` are not.
 */
class WeatherIndexScorerTest {

    @Test
    fun nominal_reproducesFixture() = assertFixture("nominal")

    @Test
    fun missingValues_reproducesFixture() = assertFixture("missing-values")

    @Test
    fun pressureTrendBoundary_reproducesFixture() = assertFixture("pressure-trend-boundary")

    @Test
    fun dstFallBack_reproducesFixture() = assertFixture("dst-fall-back")

    @Test
    fun dstSpringForward_reproducesFixture() = assertFixture("dst-spring-forward")

    // --- focused behaviour checks (documented in the contract) ---

    @Test
    fun subScoresClampToZeroTen() {
        // Warm + dry + flat pressure clamps temperature and humidity to 0.
        val request = ScoringRequest(
            zoneId = "America/Toronto",
            hours = (0..3).map { i ->
                HourlyObservation(
                    epoch = BASE_EPOCH + i * 3600L,
                    surfacePressureHpa = 1013.0,
                    relativeHumidityPct = 30.0, // below 40 -> negative -> clamp 0
                    temperatureC = 25.0,        // above 20 -> negative -> clamp 0
                )
            },
        )
        val last = WeatherIndexScorer.score(request).hours.last()
        assertEquals(HourStatus.AVAILABLE, last.status)
        assertEquals(0, BigDecimal("0.0").compareTo(last.subScores.humidity))
        assertEquals(0, BigDecimal("0.0").compareTo(last.subScores.temperature))
        assertEquals(0, BigDecimal("0.0").compareTo(last.subScores.pressure)) // flat pressure
    }

    @Test
    fun firstThreeHoursHaveNoTrendAndAreUnavailable() {
        val request = ScoringRequest(
            zoneId = "America/Toronto",
            hours = (0..4).map { i ->
                HourlyObservation(
                    epoch = BASE_EPOCH + i * 3600L,
                    surfacePressureHpa = 1013.0 - i,
                    relativeHumidityPct = 70.0,
                    temperatureC = 5.0,
                )
            },
        )
        val hours = WeatherIndexScorer.score(request).hours
        for (i in 0..2) {
            assertNull("hour $i trend", hours[i].pressureTrend3hHpa)
            assertEquals(HourStatus.UNAVAILABLE, hours[i].status)
            assertTrue(hours[i].unavailableReasons.any { it.contains("pressureTrend3h") })
        }
        assertEquals(HourStatus.AVAILABLE, hours[3].status)
    }

    // --- shared fixture assertion ---

    private fun assertFixture(caseName: String) {
        val request = FixtureSupport.loadRequest(caseName)
        val expected = FixtureSupport.loadExpected(caseName)
        val actual = WeatherIndexScorer.score(request)

        assertEquals("$caseName contractVersion", expected["contractVersion"], actual.contractVersion)
        assertEquals("$caseName indexVersion", expected["indexVersion"], actual.indexVersion)

        @Suppress("UNCHECKED_CAST")
        val expectedHours = expected["hours"] as List<Map<String, Any?>>
        assertEquals("$caseName hours size", expectedHours.size, actual.hours.size)
        for (i in expectedHours.indices) {
            assertHour(caseName, i, expectedHours[i], actual.hours[i])
        }

        @Suppress("UNCHECKED_CAST")
        val expectedDaily = expected["daily"] as List<Map<String, Any?>>
        assertEquals("$caseName daily size", expectedDaily.size, actual.daily.size)
        for (i in expectedDaily.indices) {
            assertDaily(caseName, i, expectedDaily[i], actual.daily[i])
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun assertHour(case: String, i: Int, expected: Map<String, Any?>, actual: HourIndex) {
        val tag = "$case hour[$i]"
        assertEquals("$tag epoch", (expected["epoch"] as BigDecimal).longValueExact(), actual.epoch)
        assertEquals("$tag instantUtc", expected["instantUtc"], actual.instantUtc)
        assertEquals("$tag local", expected["local"], actual.local)
        assertEquals(
            "$tag utcOffsetSeconds",
            (expected["utcOffsetSeconds"] as BigDecimal).intValueExact(),
            actual.utcOffsetSeconds,
        )
        assertEquals("$tag localDate", expected["localDate"], actual.localDate)

        val inputs = expected["inputs"] as Map<String, Any?>
        assertNumber("$tag inputs.surface_pressure", inputs["surface_pressure"], actual.inputs.surfacePressureHpa)
        assertNumber("$tag inputs.relative_humidity_2m", inputs["relative_humidity_2m"], actual.inputs.relativeHumidityPct)
        assertNumber("$tag inputs.temperature_2m", inputs["temperature_2m"], actual.inputs.temperatureC)

        assertNumber("$tag pressureTrend3h_hPa", expected["pressureTrend3h_hPa"], actual.pressureTrend3hHpa)

        val subScores = expected["subScores"] as Map<String, Any?>
        assertNumber("$tag subScores.pressure", subScores["pressure"], actual.subScores.pressure)
        assertNumber("$tag subScores.humidity", subScores["humidity"], actual.subScores.humidity)
        assertNumber("$tag subScores.temperature", subScores["temperature"], actual.subScores.temperature)

        assertNumber("$tag indexRaw", expected["indexRaw"], actual.indexRaw)
        assertNumber("$tag indexScore", expected["indexScore"], actual.indexScore)
        assertNumber("$tag indexDisplay", expected["indexDisplay"], actual.indexDisplay)

        assertEquals("$tag status", expected["status"], actual.status.name.lowercase())

        if (expected.containsKey("unavailableReasons")) {
            assertEquals("$tag unavailableReasons", expected["unavailableReasons"], actual.unavailableReasons)
        } else {
            assertTrue("$tag expected no unavailableReasons", actual.unavailableReasons.isEmpty())
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun assertDaily(case: String, i: Int, expected: Map<String, Any?>, actual: DailyIndex) {
        val tag = "$case daily[$i]"
        assertEquals("$tag date", expected["date"], actual.date)
        assertNumber("$tag low", expected["low"], actual.low)
        assertNumber("$tag high", expected["high"], actual.high)
        assertHourRef("$tag lowHour", expected["lowHour"] as Map<String, Any?>, actual.lowHour)
        assertHourRef("$tag highHour", expected["highHour"] as Map<String, Any?>, actual.highHour)
        assertEquals("$tag lowTies", (expected["lowTies"] as BigDecimal).intValueExact(), actual.lowTies)
        assertEquals("$tag highTies", (expected["highTies"] as BigDecimal).intValueExact(), actual.highTies)
        assertEquals("$tag hoursTotal", (expected["hoursTotal"] as BigDecimal).intValueExact(), actual.hoursTotal)
        assertEquals("$tag hoursAvailable", (expected["hoursAvailable"] as BigDecimal).intValueExact(), actual.hoursAvailable)
        assertEquals("$tag status", expected["status"], actual.status.name.lowercase())
        assertEquals("$tag tieBreak", expected["tieBreak"], actual.tieBreak)
    }

    private fun assertHourRef(tag: String, expected: Map<String, Any?>, actual: HourRef) {
        assertEquals("$tag.epoch", (expected["epoch"] as BigDecimal).longValueExact(), actual.epoch)
        assertEquals("$tag.instantUtc", expected["instantUtc"], actual.instantUtc)
        assertEquals("$tag.local", expected["local"], actual.local)
    }

    private fun assertNumber(tag: String, expected: Any?, actual: Any?) {
        val expectedBd = FixtureSupport.toBigDecimal(expected)
        val actualBd = FixtureSupport.toBigDecimal(actual)
        if (expectedBd == null) {
            assertNull("$tag expected null", actualBd)
        } else {
            assertNotNull("$tag expected $expectedBd but was null", actualBd)
            assertEquals("$tag expected $expectedBd but was $actualBd", 0, expectedBd.compareTo(actualBd))
        }
    }

    private companion object {
        const val BASE_EPOCH = 1_794_276_000L
    }
}
