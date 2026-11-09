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
import java.math.BigDecimal
import java.math.RoundingMode
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
 * - Today's curve omits unavailable hours; no score is interpolated or
 *   zero-filled there (§7).
 * - Five-day **ribbon** columns fill missing hours along the score trajectory
 *   so 6am / 12pm / 6pm ticks stay clock-aligned; reported high/low and
 *   start/end still come only from real available hours.
 * - Five-day columns are the five local days **after** today (contract §2.1:
 *   "5-day outlook plus the current local day"), with start/end-of-day scores
 *   from the first and last available hours, while [DailyIndex] extrema stay
 *   available for the widget (§8.2–§8.4).
 */
internal object OutlookMapper {
    const val UPCOMING_DAY_COUNT = 5

    private const val SECONDS_PER_HOUR = 3_600L
    private const val RIBBON_LERP_SCALE = 6
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
        personalResult: IndexResult? = null,
    ): OutlookUiState.Available {
        val today = referenceNow.atZone(zoneId).toLocalDate()
        val dailyByDate = indexResult.daily.associateBy { LocalDate.parse(it.date) }
        val personalDailyByDate = personalResult?.daily
            ?.associateBy { LocalDate.parse(it.date) }
            .orEmpty()
        return OutlookUiState.Available(
            today = dayCurve(
                hours = indexResult.hours,
                daily = dailyByDate[today],
                zoneId = zoneId,
                date = today,
                referenceNow = referenceNow,
                personalHours = personalResult?.hours,
            ),
            upcoming = upcomingDays(
                dailyByDate = dailyByDate,
                hours = indexResult.hours,
                zoneId = zoneId,
                today = today,
                personalDailyByDate = personalDailyByDate,
                personalHours = personalResult?.hours.orEmpty(),
            ),
        )
    }

    fun dayCurve(
        hours: List<HourIndex>,
        daily: DailyIndex?,
        zoneId: ZoneId,
        date: LocalDate,
        referenceNow: Instant,
        personalHours: List<HourIndex>? = null,
    ): DayCurve {
        val startEpoch = date.atStartOfDay(zoneId).toEpochSecond()
        val endEpoch = date.plusDays(1).atStartOfDay(zoneId).toEpochSecond()
        val points = curvePoints(hours, startEpoch, endEpoch, zoneId)
        val personalPoints = personalHours?.let { curvePoints(it, startEpoch, endEpoch, zoneId) }
            .orEmpty()
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
            personalSegments = splitAtGaps(personalPoints),
        )
    }

    private fun curvePoints(
        hours: List<HourIndex>,
        startEpoch: Long,
        endEpoch: Long,
        zoneId: ZoneId,
    ): List<CurvePoint> =
        hours
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

    fun upcomingDays(
        dailyByDate: Map<LocalDate, DailyIndex>,
        hours: List<HourIndex>,
        zoneId: ZoneId,
        today: LocalDate,
        personalDailyByDate: Map<LocalDate, DailyIndex> = emptyMap(),
        personalHours: List<HourIndex> = emptyList(),
    ): List<OutlookDay> =
        (1..UPCOMING_DAY_COUNT).map { offset ->
            val date = today.plusDays(offset.toLong())
            val available = availableScoresForDay(hours, zoneId, date)
            val personalRange = personalDailyByDate[date]?.let { toRange(it, zoneId) }
            OutlookDay(
                date = date,
                dayLabel = dayFormatter.format(date),
                range = dailyByDate[date]?.let { toRange(it, zoneId) },
                startScore = available.firstOrNull(),
                endScore = available.lastOrNull(),
                hourScores = ribbonScoresForDay(hours, zoneId, date),
                personalRange = personalRange,
                personalHourScores = if (personalRange == null || personalHours.isEmpty()) {
                    emptyList()
                } else {
                    ribbonScoresForDay(personalHours, zoneId, date)
                },
            )
        }

    /**
     * Chronological available hourly scores for [date] in [zoneId].
     *
     * Unavailable or missing hours are omitted so start/end never invent a
     * zero (contract §7).
     */
    fun availableScoresForDay(
        hours: List<HourIndex>,
        zoneId: ZoneId,
        date: LocalDate,
    ): List<BigDecimal> {
        val startEpoch = date.atStartOfDay(zoneId).toEpochSecond()
        val endEpoch = date.plusDays(1).atStartOfDay(zoneId).toEpochSecond()
        return hours
            .filter { it.epoch in startEpoch until endEpoch }
            .filter { it.status == HourStatus.AVAILABLE && it.indexScore != null }
            .sortedBy { it.epoch }
            .map { it.indexScore!! }
    }

    /**
     * One score per local hour slot for the five-day ribbon.
     *
     * Known available hours keep their scores; gaps are filled by averaging
     * along the trajectory between neighbouring anchors so vertical position
     * matches clock time (including 23- and 25-hour DST days). Leading and
     * trailing gaps hold the nearest known score. Empty when the day has no
     * available hour.
     */
    fun ribbonScoresForDay(
        hours: List<HourIndex>,
        zoneId: ZoneId,
        date: LocalDate,
    ): List<BigDecimal> {
        val startEpoch = date.atStartOfDay(zoneId).toEpochSecond()
        val endEpoch = date.plusDays(1).atStartOfDay(zoneId).toEpochSecond()
        val slotCount = ((endEpoch - startEpoch) / SECONDS_PER_HOUR).toInt()
        if (slotCount <= 0) return emptyList()
        val knownBySlot = linkedMapOf<Int, BigDecimal>()
        hours
            .filter { it.epoch in startEpoch until endEpoch }
            .filter { it.status == HourStatus.AVAILABLE && it.indexScore != null }
            .sortedBy { it.epoch }
            .forEach { hour ->
                val slot = ((hour.epoch - startEpoch) / SECONDS_PER_HOUR).toInt()
                if (slot in 0 until slotCount) {
                    knownBySlot[slot] = hour.indexScore!!
                }
            }
        return fillRibbonTrajectory(slotCount, knownBySlot)
    }

    /**
     * Fills [slotCount] ribbon samples from sparse [knownBySlot] anchors.
     *
     * Interior gaps use a linear average along the path between anchors;
     * edges hold the nearest anchor. Returns empty when there are no anchors.
     */
    fun fillRibbonTrajectory(
        slotCount: Int,
        knownBySlot: Map<Int, BigDecimal>,
    ): List<BigDecimal> {
        if (slotCount <= 0 || knownBySlot.isEmpty()) return emptyList()
        val anchors = knownBySlot.keys.filter { it in 0 until slotCount }.sorted()
        if (anchors.isEmpty()) return emptyList()
        val filled = Array<BigDecimal?>(slotCount) { null }
        anchors.forEach { slot -> filled[slot] = knownBySlot.getValue(slot) }
        val first = anchors.first()
        val firstScore = knownBySlot.getValue(first)
        for (slot in 0 until first) {
            filled[slot] = firstScore
        }
        for (index in 0 until anchors.lastIndex) {
            val left = anchors[index]
            val right = anchors[index + 1]
            val leftScore = knownBySlot.getValue(left)
            val rightScore = knownBySlot.getValue(right)
            val span = right - left
            if (span <= 1) continue
            for (slot in left + 1 until right) {
                val weight = slot - left
                filled[slot] = averageOnTrajectory(leftScore, rightScore, weight, span)
            }
        }
        val last = anchors.last()
        val lastScore = knownBySlot.getValue(last)
        for (slot in last + 1 until slotCount) {
            filled[slot] = lastScore
        }
        return filled.map { requireNotNull(it) }
    }

    /** Weighted average between two anchors along the ribbon trajectory. */
    private fun averageOnTrajectory(
        left: BigDecimal,
        right: BigDecimal,
        weight: Int,
        span: Int,
    ): BigDecimal {
        val leftWeight = BigDecimal(span - weight)
        val rightWeight = BigDecimal(weight)
        return left.multiply(leftWeight)
            .add(right.multiply(rightWeight))
            .divide(BigDecimal(span), RIBBON_LERP_SCALE, RoundingMode.HALF_UP)
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
