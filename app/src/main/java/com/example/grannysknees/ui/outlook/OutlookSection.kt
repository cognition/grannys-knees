package com.example.grannysknees.ui.outlook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.grannysknees.R
import com.example.grannysknees.index.DailyStatus
import com.example.grannysknees.ui.theme.GrannysKneesTheme
import com.example.grannysknees.ui.theme.LocalUseDarkTheme
import com.example.grannysknees.ui.theme.personalIndexColour
import com.example.grannysknees.ui.today.TodayScoreBand

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt


/** Floor heights so charts stay readable on short phones; they grow with leftover space. */
private val MIN_CHART_HEIGHT = 56.dp
private val MIN_RIBBON_HEIGHT = 52.dp
private val LINE_WIDTH = 2.dp
private val POINT_RADIUS = 3.dp
private val GRID_WIDTH = 1.dp
/** Solo Standard bar when Personal calibration is off. */
private val RIBBON_WIDTH = 16.dp
/** Paired Standard (left) + Personal (right) bars inside one day column. */
private val RIBBON_PAIR_WIDTH = 10.dp
private val RIBBON_PAIR_GAP = 3.dp
/** Left gutter for 6am / 12pm / 6pm so labels do not cover the day ribbons. */
private val TIME_GUTTER_WIDTH = 34.dp
/** Matches [DayRibbon] vertical inset so ticks sit on the bar, not the scores. */
private val RIBBON_VERTICAL_INSET = 4.dp
private val GRID_SCORES = listOf(0f, 5f, 10f)
private const val NOW_DASH_PX = 8f
private const val PERSONAL_DASH_PX = 10f
private const val PERSONAL_GAP_PX = 6f
private const val PARTIAL_RIBBON_ALPHA = 0.50f
private const val RIBBON_FILL_ALPHA = 0.62f
private const val RIBBON_STROKE_ALPHA = 0.92f

private const val FLAT_SCORE_EPSILON = 0.05f
private const val RIBBON_SAMPLE_COUNT = 96
private const val RIBBON_BLUR_PASSES = 3
private const val RIBBON_STROKE_WIDTH_DP = 1f
private val RIBBON_CORNER_RADIUS = 3.dp
/**
 * Personal (adjusted) ribbon: lightest at the day's low, deeper red at high.
 * Dark theme keeps a soft coral stroke so the bar edge stays readable on navy.
 */
private val PersonalRibbonLowLight = Color(0xFFF3D9D2)
private val PersonalRibbonHighLight = Color(0xFFB33A2B)
private val PersonalRibbonLowDark = Color(0xFFE8B4A8)
private val PersonalRibbonHighDark = Color(0xFF8F241C)
private val PersonalRibbonStrokeDark = Color(0xFFFFA89A)

/** Standard ribbon: lightest at low, deeper teal at high. */
private val StandardRibbonLowLight = Color(0xFFD5EEF1)
private val StandardRibbonHighLight = Color(0xFF0E7C86)
private val StandardRibbonLowDark = Color(0xFF3D6A78)
private val StandardRibbonHighDark = Color(0xFF5FCFD8)
private val StandardRibbonStrokeDark = Color(0xFF7FE0E8)

/** Which series a five-day ribbon represents. */
private enum class RibbonSeries {
    Standard,
    Personal,
}

/** 6am, 12pm, 6pm as fractions of the local day. */
private val TIME_TICK_FRACTIONS = listOf(6f / 24f, 12f / 24f, 18f / 24f)
private val EDGE_TICK_LABEL_IDS = listOf(
    R.string.outlook_tick_morning,
    R.string.outlook_tick_noon,
    R.string.outlook_tick_evening,
)
private val EDGE_TICK_LENGTH = 6.dp
private val EDGE_TICK_STROKE = 1.25.dp
private const val EDGE_TICK_ALPHA = 0.45f
private const val EDGE_TICK_LABEL_ALPHA = 0.72f
/** Relative heights inside the outlook column. */
private const val TODAY_CURVE_WEIGHT = 0.9f
private const val FIVE_DAY_WEIGHT = 1.1f
/** Gap between the two outlook cards. */
private val OUTLOOK_CARD_GAP = 6.dp
/** Slim bottom air so leftover height goes into the charts, not empty navy. */
private val OUTLOOK_BOTTOM_AIR = 6.dp
private val MAX_SCORE = TodayScoreBand.MAX_SCORE.toFloat()
private val WEEKDAY_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE", Locale.CANADA)

/**
 * Today's 24-hour index curve followed by the five-day high/low liquid columns.
 *
 * A short trailing spacer (about twice the card gap) keeps a little air above
 * the tab bar so the pair reads as complete.
 *
 * When [fillAvailableHeight] is false (landscape scroll), charts keep a readable
 * minimum height and the parent may scroll instead of stretching to fill.
 */
@Composable
internal fun OutlookSection(
    state: OutlookUiState.Available,
    modifier: Modifier = Modifier,
    fillAvailableHeight: Boolean = true,
) {
    if (fillAvailableHeight) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(OUTLOOK_CARD_GAP),
        ) {
            TodayCurveCard(
                curve = state.today,
                modifier = Modifier
                    .weight(TODAY_CURVE_WEIGHT, fill = true)
                    .fillMaxWidth(),
            )
            FiveDayCard(
                days = state.upcoming,
                modifier = Modifier
                    .weight(FIVE_DAY_WEIGHT, fill = true)
                    .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(OUTLOOK_BOTTOM_AIR))
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(OUTLOOK_CARD_GAP),
        ) {
            TodayCurveCard(
                curve = state.today,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MIN_CHART_HEIGHT * 3),
            )
            FiveDayCard(
                days = state.upcoming,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MIN_RIBBON_HEIGHT * 4),
            )
            Spacer(modifier = Modifier.height(OUTLOOK_BOTTOM_AIR))
        }
    }
}
@Composable
private fun TodayCurveCard(
    curve: DayCurve,
    modifier: Modifier = Modifier,
) {
    val points = curve.points
    val hasGaps = points.isNotEmpty() && points.size < curve.hoursInDay
    val plotWell = MaterialTheme.colorScheme.primaryContainer
    val plotOutline = MaterialTheme.colorScheme.outlineVariant
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            if (points.isEmpty()) {
                Text(
                    text = stringResource(R.string.outlook_today_empty),
                    style = MaterialTheme.typography.bodySmall,
                )
                return@Column
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .background(plotWell, RoundedCornerShape(12.dp))
                    .border(1.dp, plotOutline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (curve.personalSegments.isNotEmpty()) {
                    CurveLegend()
                }
                IndexCurve(
                    curve = curve,
                    description = chartDescription(curve),
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth()
                        .heightIn(min = MIN_CHART_HEIGHT),
                )
                AxisLabels(ticks = curve.axisTicks)
            }
            if (hasGaps) {
                Text(
                    text = stringResource(
                        R.string.outlook_hours_scored,
                        points.size,
                        curve.hoursInDay,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
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
private fun CurveLegend() {
    val weatherColour = MaterialTheme.colorScheme.primary
    val personalColour = personalIndexColour()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendSwatch(
            colour = weatherColour,
            label = stringResource(R.string.outlook_legend_weather),
        )
        LegendSwatch(
            colour = personalColour,
            label = stringResource(R.string.outlook_legend_yours),
            dashed = true,
        )
    }
}

@Composable
private fun LegendSwatch(
    colour: Color,
    label: String,
    dashed: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(width = 18.dp, height = 3.dp)) {
            drawLine(
                color = colour,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = size.height,
                pathEffect = if (dashed) {
                    PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                } else {
                    null
                },
                cap = StrokeCap.Round,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IndexCurve(
    curve: DayCurve,
    description: String,
    modifier: Modifier = Modifier,
) {
    val lineColour = MaterialTheme.colorScheme.primary
    val personalColour = personalIndexColour()
    val gridColour = MaterialTheme.colorScheme.outlineVariant
    val nowColour = MaterialTheme.colorScheme.secondary
    Canvas(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
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
        drawScoreSegments(
            segments = curve.personalSegments,
            colour = personalColour,
            radius = radius,
            offsetOf = ::offsetOf,
            dashed = true,
        )
        drawScoreSegments(
            segments = curve.segments,
            colour = lineColour,
            radius = radius,
            offsetOf = ::offsetOf,
            dashed = false,
        )
        curve.currentHourEpoch?.let { epoch ->
            curve.points.firstOrNull { it.epoch == epoch }?.let { point ->
                drawCircle(nowColour, radius, offsetOf(point))
            }
        }
    }
}

private fun DrawScope.drawScoreSegments(
    segments: List<List<CurvePoint>>,
    colour: Color,
    radius: Float,
    offsetOf: (CurvePoint) -> Offset,
    dashed: Boolean,
) {
    val dash = if (dashed) {
        PathEffect.dashPathEffect(floatArrayOf(PERSONAL_DASH_PX, PERSONAL_GAP_PX))
    } else {
        null
    }
    segments.forEach { segment ->
        if (segment.size == 1) {
            drawCircle(colour, radius * 0.75f, offsetOf(segment.single()))
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
            color = colour,
            style = Stroke(
                width = LINE_WIDTH.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = dash,
            ),
        )
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
private fun FiveDayCard(
    days: List<OutlookDay>,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val stripeA = MaterialTheme.colorScheme.surfaceContainerLowest
    val stripeB = MaterialTheme.colorScheme.primaryContainer
    val tickColour = MaterialTheme.colorScheme.onSurfaceVariant

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.outlook_five_day_heading),
                style = MaterialTheme.typography.titleSmall,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .heightIn(min = MIN_RIBBON_HEIGHT + 48.dp)
                    .border(1.dp, outline, RoundedCornerShape(8.dp)),
            ) {
                // Gutter spacer keeps day columns aligned with the ribbon row below.
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.width(TIME_GUTTER_WIDTH))
                    Row(modifier = Modifier.weight(1f, fill = true)) {
                        days.forEachIndexed { index, day ->
                            DayHeader(
                                day = day,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (index % 2 == 0) stripeA else stripeB),
                            )
                        }
                    }
                }
                HorizontalDivider(color = outline)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.width(TIME_GUTTER_WIDTH))
                    Row(modifier = Modifier.weight(1f, fill = true)) {
                        days.forEachIndexed { index, day ->
                            DayHighLow(
                                day = day,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (index % 2 == 0) stripeA else stripeB)
                                    .padding(horizontal = 2.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                // Labels + edge ticks share this band only — clear of high/low scores.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .heightIn(min = MIN_RIBBON_HEIGHT),
                ) {
                    TimeGutter(
                        colour = tickColour.copy(alpha = EDGE_TICK_LABEL_ALPHA),
                        modifier = Modifier
                            .width(TIME_GUTTER_WIDTH)
                            .fillMaxHeight()
                            .padding(vertical = RIBBON_VERTICAL_INSET),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .fillMaxHeight(),
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            days.forEachIndexed { index, day ->
                                DayRibbonCell(
                                    day = day,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(if (index % 2 == 0) stripeA else stripeB),
                                )
                            }
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = RIBBON_VERTICAL_INSET),
                        ) {
                            drawEdgeTimeTicks(
                                colour = tickColour.copy(alpha = EDGE_TICK_ALPHA),
                                tickLength = EDGE_TICK_LENGTH.toPx(),
                                strokeWidth = EDGE_TICK_STROKE.toPx(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated left gutter for 6am / 12pm / 6pm so the labels clear the ribbons.
 */
@Composable
private fun TimeGutter(
    colour: Color,
    modifier: Modifier = Modifier,
) {
    val labels = EDGE_TICK_LABEL_IDS.map { stringResource(it) }
    Layout(
        modifier = modifier.clearAndSetSemantics { },
        content = {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colour,
                    maxLines = 1,
                )
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map {
            it.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val centreY = (TIME_TICK_FRACTIONS[index] * height).roundToInt()
                val maxY = (height - placeable.height).coerceAtLeast(0)
                val y = (centreY - placeable.height / 2).coerceIn(0, maxY)
                val x = (width - placeable.width).coerceAtLeast(0)
                placeable.placeRelative(x, y)
            }
        }
    }
}

@Composable
private fun DayHeader(
    day: OutlookDay,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "${WEEKDAY_FORMATTER.format(day.date)} ${day.date.dayOfMonth}",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 6.dp),
        textAlign = TextAlign.Center,
    )
}

/** Per-day high/low scores above the ribbon band (ticks never cross these). */
@Composable
private fun DayHighLow(
    day: OutlookDay,
    modifier: Modifier = Modifier,
) {
    val description = dayDescription(day)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (day.isUnknown || day.range == null) {
            Text(
                text = stringResource(R.string.outlook_day_unknown),
                style = MaterialTheme.typography.titleLarge,
                color = muted,
            )
        } else {
            HighLowLine(
                high = day.range.high,
                low = day.range.low,
                personalHigh = day.personalRange?.high,
                personalLow = day.personalRange?.low,
            )
        }
    }
}

/** Ribbon-only cell: Standard left, Personal (adjusted) right when present. */
@Composable
private fun DayRibbonCell(
    day: OutlookDay,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (day.isUnknown || day.range == null) {
            Spacer(modifier = Modifier.fillMaxSize())
        } else if (day.hasPersonalRibbon && day.personalRange != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(RIBBON_PAIR_GAP),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DayRibbon(
                    hourScores = day.hourScores,
                    dayLow = day.range.low.toFloat(),
                    dayHigh = day.range.high.toFloat(),
                    isPartial = day.range.isPartial,
                    series = RibbonSeries.Standard,
                    modifier = Modifier
                        .width(RIBBON_PAIR_WIDTH)
                        .fillMaxHeight()
                        .heightIn(min = MIN_RIBBON_HEIGHT),
                )
                DayRibbon(
                    hourScores = day.personalHourScores,
                    dayLow = day.personalRange.low.toFloat(),
                    dayHigh = day.personalRange.high.toFloat(),
                    isPartial = day.personalRange.isPartial,
                    series = RibbonSeries.Personal,
                    modifier = Modifier
                        .width(RIBBON_PAIR_WIDTH)
                        .fillMaxHeight()
                        .heightIn(min = MIN_RIBBON_HEIGHT),
                )
            }
        } else {
            DayRibbon(
                hourScores = day.hourScores,
                dayLow = day.range.low.toFloat(),
                dayHigh = day.range.high.toFloat(),
                isPartial = day.range.isPartial,
                series = RibbonSeries.Standard,
                modifier = Modifier
                    .width(RIBBON_WIDTH)
                    .fillMaxHeight()
                    .heightIn(min = MIN_RIBBON_HEIGHT),
            )
        }
    }
}

/**
 * Standard high/low (teal) on top; Personal high/low (red) underneath when
 * calibration is active. Ribbons stay left Standard / right Personal below.
 */
@Composable
private fun HighLowLine(
    high: BigDecimal,
    low: BigDecimal,
    personalHigh: BigDecimal? = null,
    personalLow: BigDecimal? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(
                R.string.outlook_day_range,
                formatDisplayScore(high),
                formatDisplayScore(low),
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (personalHigh != null && personalLow != null) {
            Text(
                text = stringResource(
                    R.string.outlook_day_personal_range,
                    formatDisplayScore(personalHigh),
                    formatDisplayScore(personalLow),
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Day column as a rounded rectangle: morning at the top, night at the bottom.
 * Fill colour follows the hourly index — lightest at the day's low, darkest at
 * the day's high — teal for Standard, red for Personal.
 */
@Composable
private fun DayRibbon(
    hourScores: List<BigDecimal>,
    dayLow: Float,
    dayHigh: Float,
    isPartial: Boolean,
    series: RibbonSeries,
    modifier: Modifier = Modifier,
) {
    val darkTheme = LocalUseDarkTheme.current
    val lowColour: Color
    val highColour: Color
    val strokeColour: Color
    when (series) {
        RibbonSeries.Standard -> {
            lowColour = if (darkTheme) StandardRibbonLowDark else StandardRibbonLowLight
            highColour = if (darkTheme) StandardRibbonHighDark else StandardRibbonHighLight
            strokeColour = if (darkTheme) StandardRibbonStrokeDark else highColour
        }
        RibbonSeries.Personal -> {
            lowColour = if (darkTheme) PersonalRibbonLowDark else PersonalRibbonLowLight
            highColour = if (darkTheme) PersonalRibbonHighDark else PersonalRibbonHighLight
            strokeColour = if (darkTheme) PersonalRibbonStrokeDark else highColour
        }
    }
    val fillAlpha = if (isPartial) {
        PARTIAL_RIBBON_ALPHA * 0.7f
    } else if (darkTheme) {
        0.78f
    } else {
        RIBBON_FILL_ALPHA
    }
    val strokeAlpha = if (isPartial) PARTIAL_RIBBON_ALPHA + 0.2f else RIBBON_STROKE_ALPHA
    val dash = if (isPartial) PathEffect.dashPathEffect(floatArrayOf(6f, 5f)) else null
    Canvas(modifier = modifier.padding(vertical = RIBBON_VERTICAL_INSET)) {
        if (hourScores.isEmpty()) return@Canvas
        val scores = hourScores.map { it.toFloat() }
        val smoothed = smoothScores(
            scores = scores,
            sampleCount = RIBBON_SAMPLE_COUNT,
        )
        val fillBrush = ribbonScoreBrush(
            scores = if (isFlatDay(scores)) listOf(scores.first()) else smoothed,
            lowColour = lowColour.copy(alpha = fillAlpha),
            highColour = highColour.copy(alpha = fillAlpha),
            dayLow = dayLow,
            dayHigh = dayHigh,
            startY = 0f,
            endY = size.height,
        )
        val corner = CornerRadius(
            RIBBON_CORNER_RADIUS.toPx(),
            RIBBON_CORNER_RADIUS.toPx(),
        )
        drawRoundRect(brush = fillBrush, cornerRadius = corner)
        drawRoundRect(
            color = strokeColour.copy(alpha = strokeAlpha),
            cornerRadius = corner,
            style = Stroke(
                width = RIBBON_STROKE_WIDTH_DP.dp.toPx(),
                pathEffect = dash,
                join = StrokeJoin.Round,
                cap = StrokeCap.Round,
            ),
        )
    }
}

/** Vertical brush: lightest at [dayLow], darkest at [dayHigh]. */
internal fun ribbonScoreBrush(
    scores: List<Float>,
    lowColour: Color,
    highColour: Color,
    startY: Float,
    endY: Float,
    dayLow: Float,
    dayHigh: Float,
): Brush {
    if (scores.isEmpty()) {
        return Brush.verticalGradient(listOf(lowColour, lowColour), startY = startY, endY = endY)
    }
    if (scores.size == 1) {
        val colour = scoreRibbonColour(
            score = scores.first(),
            lowColour = lowColour,
            highColour = highColour,
            dayLow = dayLow,
            dayHigh = dayHigh,
        )
        return Brush.verticalGradient(listOf(colour, colour), startY = startY, endY = endY)
    }
    val last = (scores.size - 1).toFloat()
    val stops = Array(scores.size) { index ->
        val fraction = index / last
        fraction to scoreRibbonColour(
            score = scores[index],
            lowColour = lowColour,
            highColour = highColour,
            dayLow = dayLow,
            dayHigh = dayHigh,
        )
    }
    return Brush.verticalGradient(colorStops = stops, startY = startY, endY = endY)
}

/**
 * Maps an index score onto the day's low→high ribbon ends.
 *
 * [dayLow] is lightest; [dayHigh] is darkest. A flat day (equal ends) uses the
 * mid-tone so the rectangle still reads as a filled shape.
 */
internal fun scoreRibbonColour(
    score: Float,
    lowColour: Color,
    highColour: Color,
    dayLow: Float = 0f,
    dayHigh: Float = MAX_SCORE,
): Color {
    val span = dayHigh - dayLow
    val t = if (span <= FLAT_SCORE_EPSILON) {
        0.5f
    } else {
        ((score - dayLow) / span).coerceIn(0f, 1f)
    }
    return lerp(lowColour, highColour, t)
}

/**
 * 6am / 12pm / 6pm marks on the left and right edges of the graph box.
 */
private fun DrawScope.drawEdgeTimeTicks(
    colour: Color,
    tickLength: Float,
    strokeWidth: Float,
) {
    val length = tickLength.coerceAtMost(size.width / 3f)
    TIME_TICK_FRACTIONS.forEach { fraction ->
        val y = size.height * fraction
        drawLine(
            color = colour,
            start = Offset(0f, y),
            end = Offset(length, y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = colour,
            start = Offset(size.width - length, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun dayDescription(day: OutlookDay): String {
    val range = day.range
    val base = when {
        day.isUnknown || range == null -> stringResource(
            R.string.outlook_day_unavailable_content_description,
            day.dayLabel,
        )
        range.isPartial -> stringResource(
            R.string.outlook_day_partial_content_description,
            day.dayLabel,
            formatDisplayScore(range.high),
            formatDisplayScore(range.low),
            range.hoursAvailable,
            range.hoursTotal,
        )
        else -> stringResource(
            R.string.outlook_day_content_description,
            day.dayLabel,
            formatDisplayScore(range.high),
            formatDisplayScore(range.low),
        )
    }
    val personal = day.personalRange ?: return base
    return base + stringResource(
        R.string.outlook_day_personal_content_description,
        formatDisplayScore(personal.high),
        formatDisplayScore(personal.low),
    )
}

/** One-decimal display for day high/low anchors. */
private fun formatDisplayScore(score: BigDecimal): String =
    TodayScoreBand.formatScore(score)

/** True when every scored hour sits on the same index (high == low). */
internal fun isFlatDay(scores: List<Float>): Boolean {
    if (scores.isEmpty()) return true
    val first = scores.first()
    return scores.all { abs(it - first) <= FLAT_SCORE_EPSILON }
}

/**
 * Resamples hourly scores onto a fine grid, with cosine easing and a light
 * blur so the rectangle's vertical gradient softens hour-to-hour steps.
 */
internal fun smoothScores(
    scores: List<Float>,
    sampleCount: Int,
    blurPasses: Int = RIBBON_BLUR_PASSES,
): List<Float> {
    if (scores.isEmpty() || sampleCount <= 1) return scores
    if (scores.size == 1) return List(sampleCount) { scores[0] }
    val last = scores.lastIndex.toFloat()
    val resampled = (0 until sampleCount).map { sample ->
        val position = sample.toFloat() / (sampleCount - 1).toFloat() * last
        val left = position.toInt().coerceIn(0, scores.lastIndex)
        val right = (left + 1).coerceAtMost(scores.lastIndex)
        val fraction = position - left
        val eased = cosineEase(fraction)
        scores[left] + (scores[right] - scores[left]) * eased
    }
    return blurScores(resampled, passes = blurPasses)
}

/** Cosine ease between two samples (smoother than linear). */
internal fun cosineEase(fraction: Float): Float {
    val t = fraction.coerceIn(0f, 1f)
    return (1f - cos(t * PI.toFloat())) * 0.5f
}

/**
 * Wide neighbour average that softens sharp score jumps in the gradient.
 */
internal fun blurScores(values: List<Float>, passes: Int): List<Float> {
    if (values.isEmpty() || passes <= 0) return values
    var current = values
    repeat(passes) {
        current = current.mapIndexed { index, value ->
            val left2 = current.getOrElse(index - 2) { current.getOrElse(index - 1) { value } }
            val left1 = current.getOrElse(index - 1) { value }
            val right1 = current.getOrElse(index + 1) { value }
            val right2 = current.getOrElse(index + 2) { current.getOrElse(index + 1) { value } }
            (left2 + left1 * 4f + value * 6f + right1 * 4f + right2) / 16f
        }
    }
    return current
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
                OutlookDay(
                    date = LocalDate.of(2026, 11, 11),
                    dayLabel = "Wed 11",
                    range = range.copy(status = DailyStatus.COMPLETE),
                    startScore = BigDecimal("7.2"),
                    endScore = BigDecimal("4.1"),
                    hourScores = listOf(
                        BigDecimal("7.2"), BigDecimal("6.8"), BigDecimal("6.1"),
                        BigDecimal("5.4"), BigDecimal("5.0"), BigDecimal("4.8"),
                        BigDecimal("4.5"), BigDecimal("4.1"),
                    ),
                    personalRange = range.copy(
                        high = BigDecimal("7.8"),
                        low = BigDecimal("3.6"),
                        status = DailyStatus.COMPLETE,
                    ),
                    personalHourScores = listOf(
                        BigDecimal("7.8"), BigDecimal("7.2"), BigDecimal("6.4"),
                        BigDecimal("5.5"), BigDecimal("4.8"), BigDecimal("4.2"),
                        BigDecimal("3.9"), BigDecimal("3.6"),
                    ),
                ),
                OutlookDay(
                    date = LocalDate.of(2026, 11, 12),
                    dayLabel = "Thu 12",
                    range = range,
                    startScore = BigDecimal("4.0"),
                    endScore = BigDecimal("7.1"),
                    hourScores = listOf(
                        BigDecimal("4.0"), BigDecimal("4.2"), BigDecimal("4.8"),
                        BigDecimal("5.5"), BigDecimal("6.2"), BigDecimal("6.8"),
                        BigDecimal("7.1"),
                    ),
                    personalRange = range.copy(
                        high = BigDecimal("7.6"),
                        low = BigDecimal("3.8"),
                    ),
                    personalHourScores = listOf(
                        BigDecimal("3.8"), BigDecimal("4.0"), BigDecimal("4.6"),
                        BigDecimal("5.4"), BigDecimal("6.4"), BigDecimal("7.2"),
                        BigDecimal("7.6"),
                    ),
                ),
                OutlookDay(
                    date = LocalDate.of(2026, 11, 13),
                    dayLabel = "Fri 13",
                    range = null,
                ),
                OutlookDay(
                    date = LocalDate.of(2026, 11, 14),
                    dayLabel = "Sat 14",
                    range = range.copy(status = DailyStatus.COMPLETE),
                    startScore = BigDecimal("8.0"),
                    endScore = BigDecimal("3.2"),
                    hourScores = listOf(
                        BigDecimal("8.0"), BigDecimal("7.5"), BigDecimal("5.2"),
                        BigDecimal("4.0"), BigDecimal("3.6"), BigDecimal("3.4"),
                        BigDecimal("3.2"),
                    ),
                    personalRange = range.copy(
                        high = BigDecimal("8.5"),
                        low = BigDecimal("2.8"),
                        status = DailyStatus.COMPLETE,
                    ),
                    personalHourScores = listOf(
                        BigDecimal("8.5"), BigDecimal("7.8"), BigDecimal("5.0"),
                        BigDecimal("3.8"), BigDecimal("3.2"), BigDecimal("3.0"),
                        BigDecimal("2.8"),
                    ),
                ),
                OutlookDay(
                    date = LocalDate.of(2026, 11, 15),
                    dayLabel = "Sun 15",
                    range = range.copy(status = DailyStatus.COMPLETE),
                    startScore = BigDecimal("5.0"),
                    endScore = BigDecimal("6.1"),
                    hourScores = listOf(
                        BigDecimal("5.0"), BigDecimal("5.2"), BigDecimal("5.4"),
                        BigDecimal("5.6"), BigDecimal("5.8"), BigDecimal("6.0"),
                        BigDecimal("6.1"),
                    ),
                    personalRange = range.copy(
                        high = BigDecimal("6.4"),
                        low = BigDecimal("4.7"),
                        status = DailyStatus.COMPLETE,
                    ),
                    personalHourScores = listOf(
                        BigDecimal("4.7"), BigDecimal("5.0"), BigDecimal("5.3"),
                        BigDecimal("5.6"), BigDecimal("5.9"), BigDecimal("6.2"),
                        BigDecimal("6.4"),
                    ),
                )
            ),
        )
    }
}
