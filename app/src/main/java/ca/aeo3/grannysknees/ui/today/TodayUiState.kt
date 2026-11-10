package ca.aeo3.grannysknees.ui.today

import java.math.BigDecimal

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

/** Why Today cannot show an index number. */
internal enum class UnavailableReason {
    /** No forecast has been cached yet. */
    EMPTY,

    /** Cache age exceeded the stale window (contract §6). */
    EXPIRED,

    /** Current local hour missing or scored unavailable — no invented score. */
    INVALID_HOUR,
}

/**
 * Immutable UI state for the Today screen.
 *
 * The screen is deliberately decoupled from weather fetching and scoring: it
 * renders whatever a [TodayStateProvider] or repository hands it.
 */
internal sealed interface TodayUiState {
    /** Human-readable location the forecast applies to. */
    val location: String

    /** Pre-formatted "last updated" label, e.g. "Nov 4, 2:15 PM". */
    val lastUpdatedLabel: String

    /** First fetch / refresh in flight. */
    data class Loading(
        override val location: String,
        override val lastUpdatedLabel: String = "…",
    ) : TodayUiState

    /** A forecast is available (fresh or stale) and its index can be shown. */
    data class Available(
        override val location: String,
        override val lastUpdatedLabel: String,
        /** Shared Weather hourly `indexScore` (1 dp, 0–10). Shown without a "/10" suffix. */
        val score: BigDecimal,
        val freshness: ForecastFreshness,
        /**
         * Personal calibration score for the same hour, when multipliers are
         * not at defaults. Shown in brackets beside [score].
         */
        val personalScore: BigDecimal? = null,
        /** True when any forecast hour carries a snow WMO weather code. */
        val forecastHasSnow: Boolean = false,
    ) : TodayUiState {
        /** Display band derived from [score]. */
        val band: TodayScoreBand get() = TodayScoreBand.forScore(score)

        /** One-decimal label for the big Today number. */
        val scoreLabel: String get() = TodayScoreBand.formatScore(score)

        /** One-decimal personal label, or null when calibration is default. */
        val personalScoreLabel: String?
            get() = personalScore?.let { TodayScoreBand.formatScore(it) }

        /** Whether the stale affordance should be shown. */
        val isStale: Boolean get() = freshness == ForecastFreshness.Stale
    }

    /**
     * No usable forecast for the current hour.
     *
     * The screen shows an explicit empty/expired/invalid state instead of a
     * possibly misleading index — the contract forbids presenting expired
     * values as current (§6.1) and forbids inventing scores (§7).
     */
    data class Unavailable(
        override val location: String,
        override val lastUpdatedLabel: String,
        val reason: UnavailableReason = UnavailableReason.EMPTY,
    ) : TodayUiState
}
