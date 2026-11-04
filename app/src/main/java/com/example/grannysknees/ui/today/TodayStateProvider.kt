package com.example.grannysknees.ui.today

/**
 * Source of [TodayUiState] for the Today screen.
 *
 * Injecting the state keeps the screen testable and lets T06 swap in a real,
 * data-backed implementation without touching the composables.
 */
internal fun interface TodayStateProvider {
    /** The current state to render. */
    fun current(): TodayUiState
}

/**
 * Deterministic placeholder states used until real data is wired in (T06).
 *
 * They exercise every visible affordance: a fresh index, a stale index, and an
 * offline state.
 */
internal object SampleTodayStates {
    /** A recently fetched, normal forecast. */
    val fresh: TodayUiState.Available = TodayUiState.Available(
        location = "Toronto, ON",
        lastUpdatedLabel = "today at 8:45 AM",
        score = 7,
        freshness = ForecastFreshness.Fresh,
    )

    /** A usable but ageing forecast that must show the stale affordance. */
    val stale: TodayUiState.Available = TodayUiState.Available(
        location = "Toronto, ON",
        lastUpdatedLabel = "today at 5:10 AM",
        score = 4,
        freshness = ForecastFreshness.Stale,
    )

    /** No cached forecast; the screen shows an explicit offline state. */
    val offline: TodayUiState.Unavailable = TodayUiState.Unavailable(
        location = "Toronto, ON",
        lastUpdatedLabel = "not yet",
    )
}

/**
 * [TodayStateProvider] that returns a fixed sample.
 *
 * This is the default source for T05, standing in for the real data layer.
 */
internal class FakeTodayStateProvider(
    private val state: TodayUiState = SampleTodayStates.fresh,
) : TodayStateProvider {
    override fun current(): TodayUiState = state
}
