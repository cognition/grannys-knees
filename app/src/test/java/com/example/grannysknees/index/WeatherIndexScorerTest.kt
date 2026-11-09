package com.example.grannysknees.index

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Behaviour tests for the experimental `index-v2` 12-hour scorer.
 */
class WeatherIndexScorerTest {

    @Test
    fun calibrationAnchorsMapToEndsAndMidpoint() {
        assertEquals(
            0,
            BigDecimal("1.0").compareTo(
                WeatherIndexScorer.mapSignal(BigDecimal("-89.4"))
                    .setScale(1, RoundingMode.HALF_UP),
            ),
        )
        assertEquals(
            0,
            BigDecimal("10.0").compareTo(
                WeatherIndexScorer.mapSignal(BigDecimal("88.4"))
                    .setScale(1, RoundingMode.HALF_UP),
            ),
        )
        val mid = WeatherIndexScorer.mapSignal(BigDecimal.ZERO)
            .setScale(1, RoundingMode.HALF_UP)
        assertEquals(0, BigDecimal("5.5").compareTo(mid))
    }

    @Test
    fun twelveHourWindowProducesAvailableScore() {
        val request = windowRequest(
            priorPressure = 1015.0,
            priorHumidity = 40.0,
            priorTemperature = 15.0,
            nowPressure = 1010.0,
            nowHumidity = 70.0,
            nowTemperature = 5.0,
        )
        val last = WeatherIndexScorer.score(request).hours.last()

        assertEquals(HourStatus.AVAILABLE, last.status)
        assertEquals("index-v2", WeatherIndexScorer.score(request).indexVersion)
        assertNotNull(last.indexScore)
        // ΔH=30, ΔP=-5, ΔT=-10 → signal = 30 - (-5) - (-10) = 45
        val expected = WeatherIndexScorer.mapSignal(BigDecimal("45"))
            .setScale(1, RoundingMode.HALF_UP)
        assertEquals(0, expected.compareTo(last.indexScore))
    }

    @Test
    fun firstTwelveHoursAreUnavailableWithoutLeadIn() {
        val hours = (0..12).map { i ->
            HourlyObservation(
                epoch = BASE_EPOCH + i * 3600L,
                surfacePressureHpa = 1013.0,
                relativeHumidityPct = 60.0,
                temperatureC = 10.0,
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
    }

    @Test
    fun missingValueMarksHourUnavailable() {
        val hours = (0..12).map { i ->
            HourlyObservation(
                epoch = BASE_EPOCH + i * 3600L,
                surfacePressureHpa = if (i == 12) null else 1013.0,
                relativeHumidityPct = 60.0,
                temperatureC = 10.0,
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
    fun personalPressureSensitivityChangesScore() {
        val request = windowRequest(
            priorPressure = 1020.0,
            priorHumidity = 50.0,
            priorTemperature = 10.0,
            nowPressure = 1010.0,
            nowHumidity = 50.0,
            nowTemperature = 10.0,
        )
        val baseline = WeatherIndexScorer.score(request).hours.last().indexScore!!
        val personal = WeatherIndexScorer.score(
            request,
            DeltaSensitivities.fromMultipliers(2f, 1f, 1f),
        )
        val personalScore = personal.hours.last().indexScore!!

        assertEquals("index-v2-personal", personal.indexVersion)
        assertTrue(personalScore.compareTo(baseline) != 0)
    }

    @Test
    fun fixtureInputsStillScoreWithoutThrowing() {
        listOf(
            "nominal",
            "missing-values",
            "pressure-trend-boundary",
            "dst-fall-back",
            "dst-spring-forward",
        ).forEach { caseName ->
            val result = WeatherIndexScorer.score(FixtureSupport.loadRequest(caseName))
            assertEquals(caseName, "weather-index/2.0.0", result.contractVersion)
            assertEquals(caseName, "index-v2", result.indexVersion)
            assertTrue(caseName, result.hours.isNotEmpty())
        }
    }

    private fun windowRequest(
        priorPressure: Double,
        priorHumidity: Double,
        priorTemperature: Double,
        nowPressure: Double,
        nowHumidity: Double,
        nowTemperature: Double,
    ): ScoringRequest {
        val hours = (0..12).map { i ->
            val atPrior = i == 0
            val atNow = i == 12
            HourlyObservation(
                epoch = BASE_EPOCH + i * 3600L,
                surfacePressureHpa = when {
                    atPrior -> priorPressure
                    atNow -> nowPressure
                    else -> 1013.0
                },
                relativeHumidityPct = when {
                    atPrior -> priorHumidity
                    atNow -> nowHumidity
                    else -> 50.0
                },
                temperatureC = when {
                    atPrior -> priorTemperature
                    atNow -> nowTemperature
                    else -> 10.0
                },
            )
        }
        return ScoringRequest(zoneId = "America/Toronto", hours = hours)
    }

    private companion object {
        const val BASE_EPOCH: Long = 1_700_000_000L
    }
}
