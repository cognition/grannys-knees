package com.example.grannysknees.widget

import com.example.grannysknees.ui.outlook.OutlookUiState
import com.example.grannysknees.ui.today.ForecastFreshness
import com.example.grannysknees.ui.today.TodayScoreBand
import com.example.grannysknees.ui.today.TodayUiState
import com.example.grannysknees.ui.today.UnavailableReason
import java.math.BigDecimal

/** Immutable presentation model rendered by the home-screen widget. */
internal sealed interface WidgetUiModel {
    /** Human-readable location associated with the forecast. */
    val location: String

    /**
     * A usable fresh or stale score from the phone's Today presentation path,
     * plus the same outlook rows Today would draw for that cache.
     */
    data class Available(
        override val location: String,
        val lastUpdatedLabel: String,
        val score: Int,
        val band: TodayScoreBand,
        val freshness: ForecastFreshness,
        val hours: List<WidgetHourScore>,
        val days: List<WidgetDayScore>,
        /** Position of now across the local day, when the curve can mark it. */
        val nowFraction: Float? = null,
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

/** One scored hour copied from Today's curve. Missing hours are omitted, not zero. */
internal data class WidgetHourScore(
    val timeLabel: String,
    val score: BigDecimal,
    /** Position of this hour from local midnight to the next midnight, 0–1. */
    val fraction: Float = 0f,
)

/**
 * One five-day row copied from Today's outlook.
 *
 * [low] and [high] are null when that day has no index. They are never zero
 * standing in for a missing day.
 */
internal data class WidgetDayScore(
    val dayLabel: String,
    val low: BigDecimal?,
    val high: BigDecimal?,
)

/**
 * Maps the exact state shown by Today into the smaller widget presentation.
 *
 * The mapper deliberately accepts [TodayUiState] instead of raw weather data,
 * ensuring the widget cannot diverge from the phone's score or freshness rules.
 */
internal class WidgetModelMapper {
    /**
     * Converts the phone's Today and outlook states into one widget model.
     *
     * Hourly and daily numbers are copied from [outlook]. This mapper does not
     * score weather itself. A hidden outlook contributes no hours and no days.
     */
    fun from(state: TodayUiState, outlook: OutlookUiState): WidgetUiModel =
        when (state) {
            is TodayUiState.Available -> WidgetUiModel.Available(
                location = state.location,
                lastUpdatedLabel = state.lastUpdatedLabel,
                score = state.score,
                band = state.band,
                freshness = state.freshness,
                hours = hoursFrom(outlook),
                days = daysFrom(outlook),
                nowFraction = (outlook as? OutlookUiState.Available)?.today?.nowFraction,
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

    /** Available curve points only, in the order Today already sorted them. */
    private fun hoursFrom(outlook: OutlookUiState): List<WidgetHourScore> {
        val available = outlook as? OutlookUiState.Available ?: return emptyList()
        val curve = available.today
        return curve.points.map { point ->
            WidgetHourScore(
                timeLabel = point.timeLabel,
                score = point.score,
                fraction = curve.fractionOf(point.epoch),
            )
        }
    }

    /** Five-day rows, preserving a null range instead of substituting zero. */
    private fun daysFrom(outlook: OutlookUiState): List<WidgetDayScore> {
        val available = outlook as? OutlookUiState.Available ?: return emptyList()
        return available.upcoming.map { day ->
            WidgetDayScore(
                dayLabel = day.dayLabel,
                low = day.range?.low,
                high = day.range?.high,
            )
        }
    }
}
