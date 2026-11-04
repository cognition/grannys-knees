package com.example.grannysknees.ui.today

/**
 * Freshness of the forecast backing the Today screen.
 *
 * Mirrors the usable cache states from the weather/index contract (§6.1).
 * Expired/no-cache situations are modelled by [TodayUiState.Unavailable]
 * rather than a freshness value, since no index should be shown for them.
 */
internal enum class ForecastFreshness {
    /** Retrieved recently; present the index normally. */
    Fresh,

    /** Past the fresh window but still usable; the UI must flag it as stale. */
    Stale,
}

/**
 * Immutable UI state for the Today screen.
 *
 * The screen is deliberately decoupled from weather fetching and scoring: it
 * renders whatever a [TodayStateProvider] hands it, so T05 compiles and demos
 * without owning the weather (T03) or scorer (T04) packages. Real data is wired
 * in later (T06).
 */
internal sealed interface TodayUiState {
    /** Human-readable location the forecast applies to. */
    val location: String

    /** Pre-formatted "last updated" label, e.g. "today at 8:45 AM". */
    val lastUpdatedLabel: String

    /** A forecast is available (fresh or stale) and its index can be shown. */
    data class Available(
        override val location: String,
        override val lastUpdatedLabel: String,
        val score: Int,
        val freshness: ForecastFreshness,
    ) : TodayUiState {
        /** Display band derived from [score]. */
        val band: TodayScoreBand get() = TodayScoreBand.forScore(score)

        /** Whether the stale affordance should be shown. */
        val isStale: Boolean get() = freshness == ForecastFreshness.Stale
    }

    /**
     * No usable forecast is cached (offline or expired).
     *
     * The screen shows an explicit offline state instead of a possibly
     * misleading index — the contract forbids presenting stale-expired values
     * as current (§6.1).
     */
    data class Unavailable(
        override val location: String,
        override val lastUpdatedLabel: String,
    ) : TodayUiState
}
