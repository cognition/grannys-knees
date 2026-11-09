package com.example.grannysknees.ui.today

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class TodayUiStateTest {
    @Test
    fun `available state derives its band from the score`() {
        val state = TodayUiState.Available(
            location = "Toronto, ON",
            lastUpdatedLabel = "today at 8:45 AM",
            score = BigDecimal("7.0"),
            freshness = ForecastFreshness.Fresh,
        )

        assertEquals(TodayScoreBand.Rough, state.band)
        assertEquals("7.0", state.scoreLabel)
    }

    @Test
    fun `isStale reflects the freshness`() {
        val fresh = SampleTodayStates.fresh.copy(freshness = ForecastFreshness.Fresh)
        val stale = SampleTodayStates.fresh.copy(freshness = ForecastFreshness.Stale)

        assertFalse(fresh.isStale)
        assertTrue(stale.isStale)
    }

    @Test
    fun `sample states exercise each visible affordance`() {
        assertEquals(ForecastFreshness.Fresh, SampleTodayStates.fresh.freshness)
        assertTrue(
            SampleTodayStates.fresh.score.compareTo(BigDecimal(TodayScoreBand.MIN_SCORE)) >= 0,
        )
        assertTrue(
            SampleTodayStates.fresh.score.compareTo(BigDecimal(TodayScoreBand.MAX_SCORE)) <= 0,
        )

        assertTrue(SampleTodayStates.stale.isStale)
        assertTrue(
            SampleTodayStates.stale.score.compareTo(BigDecimal(TodayScoreBand.MIN_SCORE)) >= 0,
        )
        assertTrue(
            SampleTodayStates.stale.score.compareTo(BigDecimal(TodayScoreBand.MAX_SCORE)) <= 0,
        )

        assertTrue(SampleTodayStates.fresh.location.isNotBlank())
        assertTrue(SampleTodayStates.fresh.lastUpdatedLabel.isNotBlank())
        assertTrue(SampleTodayStates.offline.location.isNotBlank())
    }

    @Test
    fun `fake provider returns the configured state`() {
        val provider = FakeTodayStateProvider(SampleTodayStates.stale)

        assertEquals(SampleTodayStates.stale, provider.current())
    }

    @Test
    fun `fake provider defaults to a fresh sample`() {
        assertEquals(SampleTodayStates.fresh, FakeTodayStateProvider().current())
    }
}
