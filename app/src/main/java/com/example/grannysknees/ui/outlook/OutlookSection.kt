package com.example.grannysknees.ui.outlook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.grannysknees.R
import com.example.grannysknees.index.DailyStatus
import com.example.grannysknees.ui.theme.GrannysKneesTheme
import com.example.grannysknees.ui.today.TodayScoreBand
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.roundToInt

private val CHART_HEIGHT = 128.dp
private val LINE_WIDTH = 2.5.dp
private val POINT_RADIUS = 4.dp
private val GRID_WIDTH = 1.dp
private val RANGE_BAR_HEIGHT = 8.dp
private val DAY_LABEL_WIDTH = 72.dp
private val RANGE_LABEL_WIDTH = 104.dp
private val GRID_SCORES = listOf(0f, 5f, 10f)
private const val NOW_DASH_PX = 8f
private const val PARTIAL_RANGE_ALPHA = 0.55f
private val MAX_SCORE = TodayScoreBand.MAX_SCORE.toFloat()

/** Today's 24-hour index curve followed by the five-day index low/high. */
@Composable
internal fun OutlookSection(
    state: OutlookUiState.Available,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TodayCurveCard(curve = state.today)
        FiveDayCard(days = state.upcoming)
    }
}

@Composable
private fun TodayCurveCard(curve: DayCurve) {
    val points = curve.points
    val hasGaps = points.isNotEmpty() && points.size < curve.hoursInDay
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.outlook_today_heading),
                style = MaterialTheme.typography.titleSmall,
            )
            curve.range?.let { range ->
                Text(
                    text = stringResource(
                        R.string.outlook_today_range,
                        range.low.toPlainString(),
                        range.lowTimeLabel,
                        range.high.toPlainString(),
                        range.highTimeLabel,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (points.isEmpty()) {
                Text(
                    text = stringResource(R.string.outlook_today_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }
            IndexCurve(curve = curve, description = chartDescription(curve))
            AxisLabels(ticks = curve.axisTicks)
            if (hasGaps) {
                Text(
                    text = stringResource(
                        R.string.outlook_hours_scored,
                        points.size,
                        curve.hoursInDay,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = stringResource(R.string.outlook_gap_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun chartDescription(curve: DayCurve): String {
    val range = curve.range
    return if (range == null) {
        stringResource(
            R.string.outlook_chart_content_description,
            curve.points.size,
            curve.hoursInDay,
        )
    } else {
        stringResource(
            R.string.outlook_chart_range_content_description,
            curve.points.size,
            curve.hoursInDay,
            range.low.toPlainString(),
            range.lowTimeLabel,
            range.high.toPlainString(),
            range.highTimeLabel,
        )
    }
}

/**
 * Draws each run of available hours as its own path, so missing hours read as
 * breaks rather than a line dropping to zero.
 */
@Composable
private fun IndexCurve(curve: DayCurve, description: String) {
    val lineColour = MaterialTheme.colorScheme.primary
    val gridColour = MaterialTheme.colorScheme.outlineVariant
    val nowColour = MaterialTheme.colorScheme.tertiary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        val radius = POINT_RADIUS.toPx()
        val plotTop = radius
        val plotBottom = size.height - radius
        fun xOf(epoch: Long): Float = curve.fractionOf(epoch) * size.width
        fun yOf(score: Float): Float = plotBottom - (score / MAX_SCORE) * (plotBottom - plotTop)
        fun offsetOf(point: CurvePoint) = Offset(xOf(point.epoch), yOf(point.score.toFloat()))

        GRID_SCORES.forEach { score ->
            val y = yOf(score)
            drawLine(gridColour, Offset(0f, y), Offset(size.width, y), GRID_WIDTH.toPx())
        }
        curve.nowFraction?.let { fraction ->
            val x = fraction * size.width
            drawLine(
                color = nowColour,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = GRID_WIDTH.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(NOW_DASH_PX, NOW_DASH_PX)),
            )
        }
        curve.segments.forEach { segment ->
            if (segment.size == 1) {
                drawCircle(lineColour, radius * 0.75f, offsetOf(segment.single()))
                return@forEach
            }
            val path = Path().apply {
                val first = offsetOf(segment.first())
                moveTo(first.x, first.y)
                segment.drop(1).forEach { point ->
                    val next = offsetOf(point)
                    lineTo(next.x, next.y)
                }
            }
            drawPath(
                path = path,
                color = lineColour,
                style = Stroke(
                    width = LINE_WIDTH.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
        curve.currentHourEpoch?.let { epoch ->
            curve.points.firstOrNull { it.epoch == epoch }?.let { point ->
                drawCircle(nowColour, radius, offsetOf(point))
            }
        }
    }
}

/** Hour labels positioned at their real-time fraction of the (possibly 23/25 h) day. */
@Composable
private fun AxisLabels(ticks: List<AxisTick>) {
    val colour = MaterialTheme.colorScheme.onSurfaceVariant
    Layout(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
        content = {
            ticks.forEach { tick ->
                Text(
                    text = tick.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colour,
                )
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val width = constraints.maxWidth
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val centre = (ticks[index].fraction * width).roundToInt()
                val maxX = (width - placeable.width).coerceAtLeast(0)
                val x = (centre - placeable.width / 2).coerceIn(0, maxX)
                placeable.placeRelative(x, 0)
            }
        }
    }
}

@Composable
private fun FiveDayCard(days: List<OutlookDay>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.outlook_five_day_heading),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.outlook_five_day_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            days.forEach { day -> DayRow(day = day) }
        }
    }
}

@Composable
private fun DayRow(day: OutlookDay) {
    val description = dayDescription(day)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = day.dayLabel,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(DAY_LABEL_WIDTH),
        )
        RangeBar(range = day.range, modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier.width(RANGE_LABEL_WIDTH),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val range = day.range
            Text(
                text = if (range == null) {
                    stringResource(R.string.outlook_day_no_index)
                } else {
                    stringResource(
                        R.string.outlook_day_range,
                        range.low.toPlainString(),
                        range.high.toPlainString(),
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                color = if (range == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (range?.isPartial == true) {
                PartialTag()
            }
        }
    }
}

@Composable
private fun dayDescription(day: OutlookDay): String {
    val range = day.range
    return when {
        range == null -> stringResource(
            R.string.outlook_day_unavailable_content_description,
            day.dayLabel,
        )
        range.isPartial -> stringResource(
            R.string.outlook_day_partial_content_description,
            day.dayLabel,
            range.low.toPlainString(),
            range.high.toPlainString(),
            range.hoursAvailable,
            range.hoursTotal,
        )
        else -> stringResource(
            R.string.outlook_day_content_description,
            day.dayLabel,
            range.low.toPlainString(),
            range.high.toPlainString(),
        )
    }
}

/** Low–high span on a fixed 0–10 track; an empty track when the day has no index. */
@Composable
private fun RangeBar(range: DayRange?, modifier: Modifier = Modifier) {
    val trackColour = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val rangeColour = if (range?.isPartial == true) primary.copy(alpha = PARTIAL_RANGE_ALPHA) else primary
    Canvas(modifier = modifier.height(RANGE_BAR_HEIGHT)) {
        val corner = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = trackColour, cornerRadius = corner)
        if (range == null) return@Canvas
        val start = (range.low.toFloat() / MAX_SCORE) * size.width
        val end = (range.high.toFloat() / MAX_SCORE) * size.width
        val width = (end - start).coerceAtLeast(size.height)
        val left = start.coerceAtMost(size.width - width)
        drawRoundRect(
            color = rangeColour,
            topLeft = Offset(left, 0f),
            size = Size(width, size.height),
            cornerRadius = corner,
        )
    }
}

@Composable
private fun PartialTag() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(
            text = stringResource(R.string.outlook_partial_tag),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Preview(name = "Outlook", showBackground = true)
@Composable
private fun OutlookSectionPreview() {
    GrannysKneesTheme {
        OutlookSection(state = SampleOutlookStates.withGap)
    }
}

/** Deterministic outlook used by previews. */
internal object SampleOutlookStates {
    private const val DAY_START_EPOCH = 1_794_286_800L // 2026-11-10T00:00-05:00
    private const val HOUR = 3_600L
    private val SAMPLE_SCORES = listOf(
        "5.1", "5.0", "5.2", "5.3", "5.4", "5.3", null, null, null, null, null, null,
        null, "5.8", "6.2", "6.6", "6.9", "6.8", "6.1", "5.6", "5.0", "4.6", "4.3", "4.5",
    )

    /** A 24-hour day with a seven-hour gap and one partial upcoming day. */
    val withGap: OutlookUiState.Available by lazy {
        val points = SAMPLE_SCORES.mapIndexedNotNull { hour, score ->
            score?.let {
                CurvePoint(
                    epoch = DAY_START_EPOCH + hour * HOUR,
                    timeLabel = "%02d:00".format(hour),
                    utcOffsetSeconds = -18_000,
                    score = BigDecimal(it),
                )
            }
        }
        val range = DayRange(
            low = BigDecimal("4.3"),
            high = BigDecimal("6.9"),
            lowEpoch = DAY_START_EPOCH + 22 * HOUR,
            highEpoch = DAY_START_EPOCH + 16 * HOUR,
            lowTimeLabel = "22:00",
            highTimeLabel = "16:00",
            status = DailyStatus.PARTIAL,
            hoursAvailable = points.size,
            hoursTotal = SAMPLE_SCORES.size,
        )
        val endEpoch = DAY_START_EPOCH + SAMPLE_SCORES.size * HOUR
        OutlookUiState.Available(
            today = DayCurve(
                date = LocalDate.of(2026, 11, 10),
                startEpoch = DAY_START_EPOCH,
                endEpoch = endEpoch,
                segments = listOf(points.take(6), points.drop(6)),
                axisTicks = listOf("00", "06", "12", "18", "24").mapIndexed { index, label ->
                    AxisTick(fraction = index / 4f, label = label)
                },
                nowFraction = 16.5f / 24f,
                currentHourEpoch = DAY_START_EPOCH + 16 * HOUR,
                range = range,
            ),
            upcoming = listOf(
                OutlookDay(LocalDate.of(2026, 11, 11), "Wed 11", range.copy(status = DailyStatus.COMPLETE)),
                OutlookDay(LocalDate.of(2026, 11, 12), "Thu 12", range),
                OutlookDay(LocalDate.of(2026, 11, 13), "Fri 13", null),
            ),
        )
    }
}
