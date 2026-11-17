package com.aetherealmind.grannysknees.index

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import kotlin.math.abs

/**
 * Acceptance and behaviour tests for the slope-index scorer.
 *
 * The five rounded cases match `scripts/slope_index.py`.
 */
class WeatherIndexScorerTest {

    @Test
    fun `steady weather scores near the RC1 baseline`() {
        val flatP = List(13) { 1013.0 }
        val steady = listOf(20.0, 20.0, 20.0, 20.0)
        assertEquals(
            5.53,
            round2(WeatherIndexScorer.scoreSlope(steady, listOf(50.0, 50.0, 50.0, 50.0), steady, flatP)),
            0.0,
        )
    }

    @Test
    fun `cooling with rising humidity raises the index`() {
        val flatP = List(13) { 1013.0 }
        val t = listOf(26.0, 24.0, 22.0, 20.0)
        val h = listOf(41.0, 44.0, 47.0, 50.0)
        assertEquals(7.28, round2(WeatherIndexScorer.scoreSlope(t, h, t, flatP)), 0.0)
    }

    @Test
    fun `warming with drying lowers the index below baseline`() {
        val flatP = List(13) { 1013.0 }
        val t = listOf(14.0, 16.0, 18.0, 20.0)
        val h = listOf(59.0, 56.0, 53.0, 50.0)
        assertEquals(5.03, round2(WeatherIndexScorer.scoreSlope(t, h, t, flatP)), 0.0)
    }

    @Test
    fun `furnace stop grows the feels-like gap`() {
        val flatP = List(13) { 1013.0 }
        val t = listOf(20.0, 20.0, 20.0, 20.0)
        val gap = listOf(0.0, 1.5, 3.0, 4.5)
        val feels = t.zip(gap) { temp, g -> temp - g }
        assertTrue(abs(WeatherIndexScorer.slope4(gap[0], gap[1], gap[2], gap[3]) - 1.5) < 1e-9)
        assertEquals(
            5.90,
            round2(WeatherIndexScorer.scoreSlope(t, listOf(50.0, 50.0, 50.0, 50.0), feels, flatP)),
            0.0,
        )
    }

    @Test
    fun `rising pressure over twelve hours lowers the index`() {
        val steady = listOf(20.0, 20.0, 20.0, 20.0)
        val p = (0..12).map { i -> 1013.0 + i * 10.0 / 12.0 }
        assertEquals(
            round3(5.5254 - 0.506),
            round3(
                WeatherIndexScorer.scoreSlope(
                    steady,
                    listOf(50.0, 50.0, 50.0, 50.0),
                    steady,
                    p,
                ),
            ),
            0.0,
        )
    }

    @Test
    fun `missing feels-like drops the gap term to zero`() {
        val flatP = List(13) { 1013.0 }
        val steady = listOf(20.0, 20.0, 20.0, 20.0)
        assertEquals(
            5.53,
            round2(
                WeatherIndexScorer.scoreSlope(
                    steady,
                    listOf(50.0, 50.0, 50.0, 50.0),
                    feels4 = null,
                    pressure13 = flatP,
                ),
            ),
            0.0,
        )
    }

    @Test
    fun `first twelve hours are unavailable without pressure lead-in`() {
        val hours = (0..12).map { i ->
            HourlyObservation(
                epoch = BASE_EPOCH + i * 3600L,
                surfacePressureHpa = 1013.0,
                relativeHumidityPct = 60.0,
                temperatureC = 10.0,
                feelsLikeC = 10.0,
            )
        }
        val scored = WeatherIndexScorer.score(
            ScoringRequest(zoneId = "America/Toronto", hours = hours),
        ).hours

        for (i in 0 until 12) {
            assertEquals(HourStatus.UNAVAILABLE, scored[i].status)
            assertTrue(scored[i].unavailableReasons.any { it.contains("12-hour") })
        }
        assertEquals(HourStatus.AVAILABLE, scored[12].status)
        assertEquals("index-slope-v1", WeatherIndexScorer.score(
            ScoringRequest(zoneId = "America/Toronto", hours = hours),
        ).indexVersion)
    }

    @Test
    fun `missing pressure marks hour unavailable`() {
        val hours = (0..12).map { i ->
            HourlyObservation(
                epoch = BASE_EPOCH + i * 3600L,
                surfacePressureHpa = if (i == 12) null else 1013.0,
                relativeHumidityPct = 60.0,
                temperatureC = 10.0,
                feelsLikeC = 10.0,
            )
        }
        val last = WeatherIndexScorer.score(
            ScoringRequest(zoneId = "America/Toronto", hours = hours),
        ).hours.last()

        assertEquals(HourStatus.UNAVAILABLE, last.status)
        assertNull(last.indexScore)
        assertTrue(last.unavailableReasons.any { it.contains("surface_pressure") })
    }

    @Test
    fun `middle pressure nulls still score when endpoints are present`() {
        val hours = (0..12).map { i ->
            HourlyObservation(
                epoch = BASE_EPOCH + i * 3600L,
                surfacePressureHpa = when (i) {
                    0 -> 1020.0
                    12 -> 1010.0
                    6 -> null // middle gap must not block dP12
                    else -> 1015.0
                },
                relativeHumidityPct = 50.0,
                temperatureC = 20.0,
                feelsLikeC = 20.0,
            )
        }
        val last = WeatherIndexScorer.score(
            ScoringRequest(zoneId = "America/Toronto", hours = hours),
        ).hours.last()

        assertEquals(HourStatus.AVAILABLE, last.status)
        assertNotNull(last.indexScore)
        assertEquals(0, BigDecimal("-10.00").compareTo(last.subScores.pressure))
    }

    @Test
    fun `personal pressure sensitivity raises score when pressure falls`() {
        val request = flatWindowRequest(startPressure = 1020.0, endPressure = 1010.0)
        val baseline = WeatherIndexScorer.score(request).hours.last().indexScore!!
        val personal = WeatherIndexScorer.score(
            request,
            DeltaSensitivities.fromMultipliers(2f, 1f, 1f),
        )
        val personalScore = personal.hours.last().indexScore!!

        assertEquals("index-slope-v1-personal", personal.indexVersion)
        assertTrue(personalScore.compareTo(baseline) > 0)
    }

    @Test
    fun `personal sensitivity does not lower score on relief weather`() {
        val request = flatWindowRequest(startPressure = 1005.0, endPressure = 1015.0)
        val baseline = WeatherIndexScorer.score(request).hours.last().indexScore!!
        val personal = WeatherIndexScorer.score(
            request,
            DeltaSensitivities.fromMultipliers(2f, 1.8f, 1.4f),
        ).hours.last().indexScore!!

        assertTrue(
            "personal $personal should be at least baseline $baseline on relief weather",
            personal.compareTo(baseline) >= 0,
        )
    }

    @Test
    fun `scaleContribution amplifies provocation and dampens relief`() {
        val bad = WeatherIndexScorer.scaleContribution(BigDecimal("10"), BigDecimal("2"))
        val good = WeatherIndexScorer.scaleContribution(BigDecimal("-10"), BigDecimal("2"))
        assertEquals(0, BigDecimal("20").compareTo(bad))
        assertEquals(0, BigDecimal.ZERO.compareTo(good))
    }

    @Test
    fun `fixture inputs still score without throwing`() {
        listOf(
            "nominal",
            "missing-values",
            "pressure-trend-boundary",
            "dst-fall-back",
            "dst-spring-forward",
        ).forEach { caseName ->
            val result = WeatherIndexScorer.score(FixtureSupport.loadRequest(caseName))
            assertEquals(caseName, "slope-index/1.0.0", result.contractVersion)
            assertEquals(caseName, "index-slope-v1", result.indexVersion)
            assertTrue(caseName, result.hours.isNotEmpty())
            assertNotNull(caseName, result.hours.first())
        }
    }

    private fun flatWindowRequest(startPressure: Double, endPressure: Double): ScoringRequest {
        val hours = (0..12).map { i ->
            val pressure = startPressure + (endPressure - startPressure) * (i / 12.0)
            HourlyObservation(
                epoch = BASE_EPOCH + i * 3600L,
                surfacePressureHpa = pressure,
                relativeHumidityPct = 50.0,
                temperatureC = 20.0,
                feelsLikeC = 20.0,
            )
        }
        return ScoringRequest(zoneId = "America/Toronto", hours = hours)
    }

    private fun round2(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0

    private fun round3(value: Double): Double = kotlin.math.round(value * 1000.0) / 1000.0

    private companion object {
        const val BASE_EPOCH: Long = 1_700_000_000L
    }
}
