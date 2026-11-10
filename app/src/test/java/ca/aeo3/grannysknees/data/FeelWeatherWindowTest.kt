package ca.aeo3.grannysknees.data

import ca.aeo3.grannysknees.index.HourIndex
import ca.aeo3.grannysknees.index.HourInputs
import ca.aeo3.grannysknees.index.HourStatus
import ca.aeo3.grannysknees.index.SubScores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

/** ±3h Standard index window for Feel captures. */
class FeelWeatherWindowTest {

    @Test
    fun `empty available window yields null mean`() {
        val capture = FeelWeatherWindow.capture(
            hours = listOf(hour(epoch = 1_000L, available = false)),
            at = Instant.ofEpochSecond(1_000L),
        )
        assertNull(capture.weatherWindowMean)
    }

    @Test
    fun `means available scores inside three-hour window`() {
        val centre = 1_800L
        val hours = listOf(
            hour(epoch = centre - 4 * 3600, score = "9.0"), // outside
            hour(epoch = centre - 3600, score = "4.0"),
            hour(epoch = centre, score = "6.0"),
            hour(epoch = centre + 3600, score = "8.0"),
        )
        val capture = FeelWeatherWindow.capture(
            hours = hours,
            at = Instant.ofEpochSecond(centre),
        )
        assertEquals(6.0, capture.weatherWindowMean!!, 0.0001)
    }

    private fun hour(
        epoch: Long,
        score: String = "5.0",
        available: Boolean = true,
    ): HourIndex = HourIndex(
        epoch = epoch,
        instantUtc = "2026-11-09T00:00:00Z",
        local = "2026-11-09T00:00:00-05:00",
        utcOffsetSeconds = -18_000,
        localDate = "2026-11-09",
        inputs = HourInputs(null, null, null),
        pressureTrend3hHpa = if (available) BigDecimal("-2.00") else null,
        subScores = SubScores(
            pressure = if (available) BigDecimal("-2.00") else null,
            humidity = if (available) BigDecimal("3.00") else null,
            temperature = if (available) BigDecimal("-1.00") else null,
        ),
        indexRaw = if (available) BigDecimal(score) else null,
        indexScore = if (available) BigDecimal(score) else null,
        indexDisplay = if (available) score.toDouble().toInt() else null,
        status = if (available) HourStatus.AVAILABLE else HourStatus.UNAVAILABLE,
        unavailableReasons = if (available) emptyList() else listOf("missing"),
    )
}
