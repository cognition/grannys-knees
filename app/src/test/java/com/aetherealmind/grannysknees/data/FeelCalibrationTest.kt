package com.aetherealmind.grannysknees.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Feel time-point collapse, consecutive counting, suggest/blend, and Personal matrix.
 */
class FeelCalibrationTest {

    @Test
    fun `pain stats ignore empty and use sample stddev`() {
        val single = FeelCalibration.painStats(listOf(4))
        assertEquals(4.0, single.first, 0.0001)
        assertEquals(0.0, single.second, 0.0001)

        val pair = FeelCalibration.painStats(listOf(2, 8))
        assertEquals(5.0, pair.first, 0.0001)
        assertEquals(4.2426, pair.second, 0.01)
    }

    @Test
    fun `saves within fifteen minutes collapse into one sample`() {
        val first = sample(
            at = Instant.parse("2026-11-09T12:00:00Z"),
            ratings = listOf(3),
            observationId = "a",
        )
        val second = sample(
            at = Instant.parse("2026-11-09T12:10:00Z"),
            ratings = listOf(7),
            observationId = "b",
        )
        val merged = FeelCalibration.upsertSample(listOf(first), second)
        assertEquals(1, merged.size)
        assertEquals(listOf(3, 7), merged.single().ratings)
        assertEquals(listOf("a", "b"), merged.single().observationIds)
    }

    @Test
    fun `saves more than fifteen minutes apart stay separate`() {
        val first = sample(at = Instant.parse("2026-11-09T12:00:00Z"))
        val second = sample(at = Instant.parse("2026-11-09T12:16:00Z"))
        assertEquals(2, FeelCalibration.upsertSample(listOf(first), second).size)
    }

    @Test
    fun `consecutive count requires forty-five minutes between counted points`() {
        val samples = listOf(
            sample(at = Instant.parse("2026-11-09T10:00:00Z")),
            sample(at = Instant.parse("2026-11-09T10:30:00Z")), // <45m — skipped for count
            sample(at = Instant.parse("2026-11-09T10:50:00Z")), // ≥45m from first
        )
        val counted = FeelCalibration.consecutiveCountable(samples)
        assertEquals(2, counted.size)
        assertEquals(Instant.parse("2026-11-09T10:00:00Z"), counted[0].centreAt)
        assertEquals(Instant.parse("2026-11-09T10:50:00Z"), counted[1].centreAt)
    }

    @Test
    fun `unlock needs ten consecutive countable points`() {
        val short = (0 until 9).map { index ->
            sample(at = Instant.parse("2026-11-09T10:00:00Z").plusSeconds(index * 45L * 60L))
        }
        assertFalse(FeelCalibration.isUnlocked(short))

        val enough = short + sample(
            at = Instant.parse("2026-11-09T10:00:00Z").plusSeconds(9L * 45L * 60L),
        )
        assertTrue(FeelCalibration.isUnlocked(enough))
    }

    @Test
    fun `blend keeps three quarters of the base`() {
        val base = SensitivityProfile(pressure = 1f, humidity = 1f, cold = 1f)
        val suggested = SensitivityProfile(pressure = 2f, humidity = 2f, cold = 2f)
        val blended = FeelCalibration.blend(base, suggested)
        assertEquals(1.25f, blended.pressure, 0.0001f)
        assertEquals(1.25f, blended.humidity, 0.0001f)
        assertEquals(1.25f, blended.cold, 0.0001f)
    }

    @Test
    fun `matrix hides personal when adj unused and feel opted out`() {
        val samples = (0 until 10).map { index ->
            sample(
                at = Instant.parse("2026-11-09T10:00:00Z").plusSeconds(index * 45L * 60L),
                ratings = listOf(8),
                weather = 4.0,
                deltaPressure = -5.0,
            )
        }
        assertNull(
            PersonalIndexResolver.effectiveProfile(
                adj = SensitivityProfile.DEFAULT,
                feelOptIn = false,
                samples = samples,
            ),
        )
    }

    @Test
    fun `matrix uses adj only when feel opted out`() {
        val adj = SensitivityProfile(pressure = 1.5f, humidity = 1f, cold = 1f)
        val effective = PersonalIndexResolver.effectiveProfile(
            adj = adj,
            feelOptIn = false,
            samples = emptyList(),
        )
        assertEquals(adj, effective)
    }

    @Test
    fun `matrix feels default base when unlocked and opted in`() {
        val samples = (0 until 10).map { index ->
            sample(
                at = Instant.parse("2026-11-09T10:00:00Z").plusSeconds(index * 45L * 60L),
                ratings = listOf(9),
                weather = 3.0,
                deltaPressure = -6.0,
            )
        }
        val effective = PersonalIndexResolver.effectiveProfile(
            adj = SensitivityProfile.DEFAULT,
            feelOptIn = true,
            samples = samples,
        )
        assertTrue(effective != null)
        assertFalse(effective!!.isDefault())
    }

    private fun sample(
        at: Instant,
        ratings: List<Int> = listOf(5),
        weather: Double = 5.0,
        deltaPressure: Double? = -2.0,
        deltaHumidity: Double? = 3.0,
        deltaTemperature: Double? = -1.0,
        observationId: String = "id-${at.epochSecond}",
    ): FeelCalibrationSample = FeelCalibrationSample(
        centreAt = at,
        ratings = ratings,
        weatherWindowMean = weather,
        deltaPressure = deltaPressure,
        deltaHumidity = deltaHumidity,
        deltaTemperature = deltaTemperature,
        observationIds = listOf(observationId),
    )
}
