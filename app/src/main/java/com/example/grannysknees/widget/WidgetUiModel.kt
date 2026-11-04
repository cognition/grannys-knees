package com.example.grannysknees.widget

import com.example.grannysknees.ui.today.ForecastFreshness
import com.example.grannysknees.ui.today.TodayScoreBand
import com.example.grannysknees.ui.today.TodayUiState
import com.example.grannysknees.ui.today.UnavailableReason

/** Immutable presentation model rendered by the compact home-screen widget. */
internal sealed interface WidgetUiModel {
    /** Human-readable location associated with the forecast. */
    val location: String

    /** A usable fresh or stale score from the phone's Today presentation path. */
    data class Available(
        override val location: String,
        val lastUpdatedLabel: String,
        val score: Int,
        val band: TodayScoreBand,
        val freshness: ForecastFreshness,
    ) : WidgetUiModel {
        /** Whether the widget must show the stale badge. */
        val isStale: Boolean
            get() = freshness == ForecastFreshness.Stale
    }

    /** An expired, empty, or invalid forecast for which no score may be shown. */
    data class Unavailable(
        override val location: String,
        val reason: UnavailableReason,
    ) : WidgetUiModel
}

/**
 * Maps the exact state shown by Today into the smaller widget presentation.
 *
 * The mapper deliberately accepts [TodayUiState] instead of raw weather data,
 * ensuring the widget cannot diverge from the phone's score or freshness rules.
 */
internal class WidgetModelMapper {
    /** Converts a phone presentation state into a compact widget model. */
    fun fromToday(state: TodayUiState): WidgetUiModel =
        when (state) {
            is TodayUiState.Available -> WidgetUiModel.Available(
                location = state.location,
                lastUpdatedLabel = state.lastUpdatedLabel,
                score = state.score,
                band = state.band,
                freshness = state.freshness,
            )

            is TodayUiState.Unavailable -> WidgetUiModel.Unavailable(
                location = state.location,
                reason = state.reason,
            )

            is TodayUiState.Loading -> WidgetUiModel.Unavailable(
                location = state.location,
                reason = UnavailableReason.EMPTY,
            )
        }
}
