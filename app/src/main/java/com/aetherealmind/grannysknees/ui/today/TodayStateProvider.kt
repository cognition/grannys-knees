package com.aetherealmind.grannysknees.ui.today

import java.math.BigDecimal

/**
 * Source of [TodayUiState] for the Today screen.
 *
 * Injecting the state keeps the screen testable. Production R0 uses
 * [com.aetherealmind.grannysknees.data.ForecastRepository] via composition-root
 * wiring; [FakeTodayStateProvider] remains for previews and unit tests.
 */
internal fun interface TodayStateProvider {
    /** The current state to render. */
    fun current(): TodayUiState
}

/**
 * Deterministic placeholder states used in previews and UI unit tests.
 */
internal object SampleTodayStates {
    /** A recently fetched, normal forecast. */
    val fresh: TodayUiState.Available = TodayUiState.Available(
        location = "Toronto, ON",
        lastUpdatedLabel = "today at 8:45 AM",
        score = BigDecimal("7.0"),
        freshness = ForecastFreshness.Fresh,
    )

    /** A usable but ageing forecast that must show the stale affordance. */
    val stale: TodayUiState.Available = TodayUiState.Available(
        location = "Toronto, ON",
        lastUpdatedLabel = "today at 5:10 AM",
        score = BigDecimal("4.0"),
        freshness = ForecastFreshness.Stale,
    )

    /** No cached forecast; the screen shows an explicit offline state. */
    val offline: TodayUiState.Unavailable = TodayUiState.Unavailable(
        location = "Toronto, ON",
        lastUpdatedLabel = "not yet",
        reason = UnavailableReason.EMPTY,
    )

    /** Cache past the stale window — index must be hidden. */
    val expired: TodayUiState.Unavailable = TodayUiState.Unavailable(
        location = "Toronto, ON",
        lastUpdatedLabel = "today at 5:10 AM",
        reason = UnavailableReason.EXPIRED,
    )
}

/**
 * [TodayStateProvider] that returns a fixed sample.
 */
internal class FakeTodayStateProvider(
    private val state: TodayUiState = SampleTodayStates.fresh,
) : TodayStateProvider {
    override fun current(): TodayUiState = state
}
