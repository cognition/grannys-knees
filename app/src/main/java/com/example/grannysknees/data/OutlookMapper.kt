package com.example.grannysknees.data

import com.example.grannysknees.index.DailyIndex
import com.example.grannysknees.index.HourIndex
import com.example.grannysknees.index.HourStatus
import com.example.grannysknees.index.IndexResult
import com.example.grannysknees.ui.outlook.AxisTick
import com.example.grannysknees.ui.outlook.CurvePoint
import com.example.grannysknees.ui.outlook.DayCurve
import com.example.grannysknees.ui.outlook.DayRange
import com.example.grannysknees.ui.outlook.OutlookDay
import com.example.grannysknees.ui.outlook.OutlookUiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Projects a scored [IndexResult] into [OutlookUiState.Available].
 *
 * - The curve covers the **current local calendar day** of [ZoneId] (not a
 *   rolling 24 hours), so it lines up with the daily low/high.
 * - Day membership, labels and x positions come from each hour's epoch
 *   converted through the forecast zone, never from a fixed 24-row block, so
 *   23- and 25-hour DST days are drawn honestly (contract §4.2).
 * - Unavailable hours are omitted; no score is interpolated or zero-filled (§7).
 * - Five-day rows are the five local days **after** today (contract §2.1:
 *   "5-day outlook plus the current local day"), reusing the scorer's
 *   [DailyIndex] extrema and tie-break unchanged (§8.2–§8.4).
 */
internal object OutlookMapper {
    const val UPCOMING_DAY_COUNT = 5

    private const val SECONDS_PER_HOUR = 3_600L
    private val AXIS_TICK_HOURS = listOf(0, 6, 12, 18)
    private const val END_OF_DAY_TICK_LABEL = "24"

    private val tickFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH", Locale.CANADA)
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CANADA)
    private val ambiguousTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm '('xxx')'", Locale.CANADA)
    private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d", Locale.CANADA)

    fun toOutlook(
        indexResult: IndexResult,
        zoneId: ZoneId,
        referenceNow: Instant,
    ): OutlookUiState.Available {
        val today = referenceNow.atZone(zoneId).toLocalDate()
        val dailyByDate = indexResult.daily.associateBy { LocalDate.parse(it.date) }
        return OutlookUiState.Available(
            today = dayCurve(indexResult.hours, dailyByDate[today], zoneId, today, referenceNow),
            upcoming = upcomingDays(dailyByDate, zoneId, today),
        )
    }

    fun dayCurve(
        hours: List<HourIndex>,
        daily: DailyIndex?,
        zoneId: ZoneId,
        date: LocalDate,
        referenceNow: Instant,
    ): DayCurve {
        val startEpoch = date.atStartOfDay(zoneId).toEpochSecond()
        val endEpoch = date.plusDays(1).atStartOfDay(zoneId).toEpochSecond()
        val points = hours
            .filter { it.epoch in startEpoch until endEpoch }
            .filter { it.status == HourStatus.AVAILABLE && it.indexScore != null }
            .sortedBy { it.epoch }
            .map { hour ->
                CurvePoint(
                    epoch = hour.epoch,
                    timeLabel = formatTime(hour.epoch, zoneId),
                    utcOffsetSeconds = hour.utcOffsetSeconds,
                    score = hour.indexScore!!,
                )
            }
        val fractionOf = { epoch: Long -> DayCurve.fraction(epoch, startEpoch, endEpoch) }
        val nowEpoch = referenceNow.epochSecond
        val nowInDay = nowEpoch in startEpoch until endEpoch
        val currentHourEpoch = if (nowInDay) {
            referenceNow.atZone(zoneId).withMinute(0).withSecond(0).withNano(0).toEpochSecond()
        } else {
            null
        }
        return DayCurve(
            date = date,
            startEpoch = startEpoch,
            endEpoch = endEpoch,
            segments = splitAtGaps(points),
            axisTicks = axisTicks(date, zoneId, fractionOf),
            nowFraction = if (nowInDay) fractionOf(nowEpoch) else null,
            currentHourEpoch = currentHourEpoch?.takeIf { epoch -> points.any { it.epoch == epoch } },
            range = daily?.let { toRange(it, zoneId) },
        )
    }

    fun upcomingDays(
        dailyByDate: Map<LocalDate, DailyIndex>,
        zoneId: ZoneId,
        today: LocalDate,
    ): List<OutlookDay> =
        (1..UPCOMING_DAY_COUNT).map { offset ->
            val date = today.plusDays(offset.toLong())
            OutlookDay(
                date = date,
                dayLabel = dayFormatter.format(date),
                range = dailyByDate[date]?.let { toRange(it, zoneId) },
            )
        }

    /**
     * Local wall time for [epoch]; on a fall-back day the repeated hour gets
     * its UTC offset so the two `01:00` rows stay distinguishable.
     */
    fun formatTime(epoch: Long, zoneId: ZoneId): String {
        val zoned = Instant.ofEpochSecond(epoch).atZone(zoneId)
        val ambiguous = zoneId.rules.getValidOffsets(zoned.toLocalDateTime()).size > 1
        return (if (ambiguous) ambiguousTimeFormatter else timeFormatter).format(zoned)
    }

    private fun splitAtGaps(points: List<CurvePoint>): List<List<CurvePoint>> {
        val segments = mutableListOf<MutableList<CurvePoint>>()
        for (point in points) {
            val current = segments.lastOrNull()
            if (current != null && point.epoch - current.last().epoch == SECONDS_PER_HOUR) {
                current += point
            } else {
                segments += mutableListOf(point)
            }
        }
        return segments
    }

    private fun axisTicks(
        date: LocalDate,
        zoneId: ZoneId,
        fractionOf: (Long) -> Float,
    ): List<AxisTick> {
        val hourTicks = AXIS_TICK_HOURS.map { hour ->
            val tick = ZonedDateTime.of(date, LocalTime.of(hour, 0), zoneId)
            AxisTick(fraction = fractionOf(tick.toEpochSecond()), label = tickFormatter.format(tick))
        }
        return hourTicks + AxisTick(fraction = 1f, label = END_OF_DAY_TICK_LABEL)
    }

    private fun toRange(daily: DailyIndex, zoneId: ZoneId): DayRange =
        DayRange(
            low = daily.low,
            high = daily.high,
            lowEpoch = daily.lowHour.epoch,
            highEpoch = daily.highHour.epoch,
            lowTimeLabel = formatTime(daily.lowHour.epoch, zoneId),
            highTimeLabel = formatTime(daily.highHour.epoch, zoneId),
            status = daily.status,
            hoursAvailable = daily.hoursAvailable,
            hoursTotal = daily.hoursTotal,
        )
}
