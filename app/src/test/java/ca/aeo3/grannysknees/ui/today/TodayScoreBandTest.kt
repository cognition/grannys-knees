package ca.aeo3.grannysknees.ui.today

import org.junit.Assert.assertEquals
import org.junit.Test

class TodayScoreBandTest {
    @Test
    fun `every in-range score maps to the contract band`() {
        val expected = mapOf(
            0 to TodayScoreBand.Calm,
            1 to TodayScoreBand.Calm,
            2 to TodayScoreBand.Calm,
            3 to TodayScoreBand.Mild,
            4 to TodayScoreBand.Mild,
            5 to TodayScoreBand.Noticeable,
            6 to TodayScoreBand.Noticeable,
            7 to TodayScoreBand.Rough,
            8 to TodayScoreBand.Rough,
            9 to TodayScoreBand.Stormy,
            10 to TodayScoreBand.Stormy,
        )

        expected.forEach { (score, band) ->
            assertEquals("score $score", band, TodayScoreBand.forScore(score))
        }
    }

    @Test
    fun `bands span the whole 0 to 10 range without gaps`() {
        val covered = TodayScoreBand.entries.flatMap { it.scores }.toSet()

        assertEquals((TodayScoreBand.MIN_SCORE..TodayScoreBand.MAX_SCORE).toSet(), covered)
    }

    @Test
    fun `out of range scores clamp into the supported window`() {
        assertEquals(TodayScoreBand.Calm, TodayScoreBand.forScore(-5))
        assertEquals(TodayScoreBand.Stormy, TodayScoreBand.forScore(99))
    }
}
