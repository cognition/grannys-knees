package com.example.grannysknees.ui.outlook

import com.example.grannysknees.index.DailyStatus
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Outlook shown beneath the Today index: a curve of the hourly `indexScore`
 * across the current **local calendar day**, plus index low/high for the next
 * five local days (contract §8).
 *
 * Only presented while the cache is fresh or stale (§6.1); expired or empty
 * caches hide it rather than showing old values as current.
 */
internal sealed interface OutlookUiState {
    /** No usable cache, so nothing is drawn. */
    data object Hidden : OutlookUiState

    /** Usable scored forecast for the outlook. */
    data class Available(
        val today: DayCurve,
        val upcoming: List<OutlookDay>,
    ) : OutlookUiState
}

/**
 * Hourly index curve for one local calendar day.
 *
 * The x axis spans real elapsed time from local midnight [startEpoch] to the
 * next local midnight [endEpoch], so a DST day is 23 or 25 hours wide rather
 * than a fixed block of 24 (contract §4.2).
 *
 * [segments] are runs of consecutive **available** hours; unavailable or
 * missing hours are omitted and appear as breaks between segments, never as
 * zero (contract §7).
 */
internal data class DayCurve(
    val date: LocalDate,
    val startEpoch: Long,
    val endEpoch: Long,
    val segments: List<List<CurvePoint>>,
    val axisTicks: List<AxisTick>,
    /** Position of "now" across the day (0–1), or `null` when outside it. */
    val nowFraction: Float?,
    /** Epoch of the current local hour when it has an available score. */
    val currentHourEpoch: Long?,
    /** Today's index low/high, or `null` when no hour is available. */
    val range: DayRange?,
) {
    /** Wall-clock hours in this local day: 23, 24 or 25. */
    val hoursInDay: Int get() = ((endEpoch - startEpoch) / SECONDS_PER_HOUR).toInt()

    /** All available points in epoch order. */
    val points: List<CurvePoint> get() = segments.flatten()

    /** Horizontal position (0–1) of [epoch] within the day. */
    fun fractionOf(epoch: Long): Float = fraction(epoch, startEpoch, endEpoch)

    companion object {
        private const val SECONDS_PER_HOUR = 3_600L

        /** Position of [epoch] between [startEpoch] and [endEpoch] as 0–1. */
        fun fraction(epoch: Long, startEpoch: Long, endEpoch: Long): Float =
            ((epoch - startEpoch).toDouble() / (endEpoch - startEpoch).toDouble()).toFloat()
    }
}

/** One available hour on the curve. */
internal data class CurvePoint(
    val epoch: Long,
    /** Local wall time, e.g. `16:00`, with the offset appended on a repeated hour. */
    val timeLabel: String,
    val utcOffsetSeconds: Int,
    /** Hourly `indexScore` (1 dp, 0–10). */
    val score: BigDecimal,
)

/** Axis label placed at [fraction] (0–1) of the day's width. */
internal data class AxisTick(
    val fraction: Float,
    val label: String,
)

/**
 * Index low/high for a local day, taken directly from the scorer's
 * [com.example.grannysknees.index.DailyIndex] so extrema and the earliest-epoch
 * tie-break match contract §8.4.
 */
internal data class DayRange(
    val low: BigDecimal,
    val high: BigDecimal,
    val lowEpoch: Long,
    val highEpoch: Long,
    val lowTimeLabel: String,
    val highTimeLabel: String,
    val status: DailyStatus,
    val hoursAvailable: Int,
    val hoursTotal: Int,
) {
    /** Fewer than the contract's complete-day threshold of available hours. */
    val isPartial: Boolean get() = status == DailyStatus.PARTIAL
}

/**
 * One row of the five-day outlook. [range] is `null` when the scorer produced
 * no available hour for that date — the row says so instead of inventing one.
 */
internal data class OutlookDay(
    val date: LocalDate,
    val dayLabel: String,
    val range: DayRange?,
)
