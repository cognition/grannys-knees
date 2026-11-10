package com.example.grannysknees.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.DpSize
import com.example.grannysknees.MainActivity
import com.example.grannysknees.R
import com.example.grannysknees.ui.today.TodayScoreBand

/**
 * Which widget layout fits the cell the user has placed.
 *
 * Breakpoints match [GrannysKneesWidget.sizeMode]. A short cell stays on the
 * current index. A wider cell adds scored hours. A tall cell adds the
 * five-day low/high. Missing hours and days are omitted, never drawn as zero.
 */
internal enum class WidgetLayout {
    Current,
    Hourly,
    FiveDay,
}

/** Picks a layout from the size Glance reports for this instance. */
internal object WidgetLayoutPicker {
    val hourlyMinWidth: Dp = 240.dp
    val fiveDayMinHeight: Dp = 220.dp

    /** Tallest applicable layout wins so a large cell can show the five-day rows. */
    fun pick(width: Dp, height: Dp): WidgetLayout =
        when {
            height >= fiveDayMinHeight -> WidgetLayout.FiveDay
            width >= hourlyMinWidth -> WidgetLayout.Hourly
            else -> WidgetLayout.Current
        }
}

/**
 * Home-screen widget for the cached knee-weather index.
 *
 * Each update, including the system update after reboot, calls [provideGlance],
 * which re-reads the same cache file as Today. This class does not fetch the
 * network and does not score hours on its own.
 */
internal class GrannysKneesWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(168.dp, 72.dp),
            DpSize(280.dp, 160.dp),
            DpSize(280.dp, 280.dp),
        ),
    )

    /** Loads one coherent model before providing the widget composition. */
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = WidgetModelLoader(context).load()
        provideContent {
            WidgetContentRenderer.Content(model)
        }
    }
}

/** Android receiver registered for widget placement and periodic updates. */
class GrannysKneesWidgetReceiver : GlanceAppWidgetReceiver() {
    /** Glance implementation responsible for every registered widget instance. */
    override val glanceAppWidget: GlanceAppWidget = GrannysKneesWidget()
}

/** Renders fresh, stale, and unavailable compact widget states. */
private object WidgetContentRenderer {
    private val backgroundColour = ColorProvider(Color(0xFFF4F8F9))
    private val primaryColour = ColorProvider(Color(0xFF0E7C86))
    private val personalColour = ColorProvider(Color(0xFFC24E2D))
    private val secondaryColour = ColorProvider(Color(0xFF5E7380))
    private val badgeBackgroundColour = ColorProvider(Color(0xFFF8E4E1))
    private val badgeTextColour = ColorProvider(Color(0xFFC24E2D))


    /** Draws the full tappable widget surface at the size the launcher allocated. */
    @Composable
    fun Content(model: WidgetUiModel) {
        val context = LocalContext.current
        val size = LocalSize.current
        val layout = WidgetLayoutPicker.pick(size.width, size.height)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColour)
                .cornerRadius(WIDGET_CORNER_RADIUS)
                .clickable(
                    actionStartActivity(
                        Intent(context, MainActivity::class.java),
                    ),
                )
                .padding(WIDGET_PADDING),
        ) {
            when (model) {
                is WidgetUiModel.Available -> when (layout) {
                    WidgetLayout.Current -> AvailableContent(model)
                    WidgetLayout.Hourly -> HourlyContent(model)
                    WidgetLayout.FiveDay -> FiveDayContent(model)
                }
                is WidgetUiModel.Unavailable -> UnavailableContent(model)
            }
        }
    }

    /** Draws an index that is safe to present as current or stale. */
    @Composable
    private fun AvailableContent(model: WidgetUiModel.Available) {
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.Start,
        ) {
            Text(
                text = model.location,
                style = TextStyle(
                    color = secondaryColour,
                    fontSize = LOCATION_TEXT_SIZE,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                ScorePair(model)
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = context.getString(bandLabelResource(model.band)),
                    style = TextStyle(
                        color = primaryColour,
                        fontSize = BAND_TEXT_SIZE,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
                if (model.isStale) {
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    StaleBadge()
                }
            }
            Text(
                text = context.getString(R.string.widget_updated, model.lastUpdatedLabel),
                style = TextStyle(
                    color = secondaryColour,
                    fontSize = META_TEXT_SIZE,
                ),
                maxLines = 1,
            )
        }
    }

    /** Draws the required stale affordance beside an otherwise usable score. */
    @Composable
    private fun StaleBadge() {
        val context = LocalContext.current
        Box(
            modifier = GlanceModifier
                .background(badgeBackgroundColour)
                .cornerRadius(BADGE_CORNER_RADIUS)
                .padding(horizontal = 7.dp, vertical = 3.dp),
        ) {
            Text(
                text = context.getString(R.string.widget_stale_badge),
                style = TextStyle(
                    color = badgeTextColour,
                    fontSize = BADGE_TEXT_SIZE,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }

    /** Draws scored hours under the current index. Hours Today omitted are absent. */
    @Composable
    private fun HourlyContent(model: WidgetUiModel.Available) {
        val context = LocalContext.current
        Column(modifier = GlanceModifier.fillMaxSize()) {
            ScoreLine(model)
            Spacer(modifier = GlanceModifier.height(4.dp))
            val curve = WidgetDayCurveBitmap.draw(model)
            if (curve == null) {
                MetaText(context.getString(R.string.widget_no_hours))
            } else {
                Image(
                    provider = ImageProvider(curve),
                    contentDescription = context.getString(R.string.widget_day_curve),
                    modifier = GlanceModifier.fillMaxWidth().height(72.dp),
                )
            }
        }
    }

    /** Draws the five-day index low/high under the current index. */
    @Composable
    private fun FiveDayContent(model: WidgetUiModel.Available) {
        val context = LocalContext.current
        Column(modifier = GlanceModifier.fillMaxSize()) {
            ScoreLine(model)
            Spacer(modifier = GlanceModifier.height(4.dp))
            if (model.days.isEmpty()) {
                MetaText(context.getString(R.string.widget_no_hours))
            } else {
                model.days.forEach { day ->
                    DayRow(day)
                }
            }
        }
    }

    /** Current score and stale badge, shared by the larger layouts. */
    @Composable
    private fun ScoreLine(model: WidgetUiModel.Available) {
        val context = LocalContext.current
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            ScorePair(model)
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = context.getString(bandLabelResource(model.band)),
                style = TextStyle(
                    color = primaryColour,
                    fontSize = BAND_TEXT_SIZE,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            if (model.isStale) {
                Spacer(modifier = GlanceModifier.width(8.dp))
                StaleBadge()
            }
        }
    }

    /** Standard score, plus Personal in brackets when Today would show one. */
    @Composable
    private fun ScorePair(model: WidgetUiModel.Available) {
        Text(
            text = TodayScoreBand.formatScore(model.score),
            style = TextStyle(
                color = primaryColour,
                fontSize = SCORE_TEXT_SIZE,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        val personal = model.personalScore
        if (personal != null) {
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                text = "(${TodayScoreBand.formatScore(personal)})",
                style = TextStyle(
                    color = personalColour,
                    fontSize = SCORE_TEXT_SIZE,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }

    /** One five-day row. A missing range says so and does not print zero. */
    @Composable
    private fun DayRow(day: WidgetDayScore) {
        val context = LocalContext.current
        val rangeText = if (day.low != null && day.high != null) {
            context.getString(
                R.string.widget_day_range,
                day.low.toPlainString(),
                day.high.toPlainString(),
            )
        } else {
            context.getString(R.string.widget_day_no_index)
        }
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = day.dayLabel,
                style = TextStyle(color = secondaryColour, fontSize = META_TEXT_SIZE),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = rangeText,
                style = TextStyle(
                    color = primaryColour,
                    fontSize = META_TEXT_SIZE,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
        }
    }

    /** Secondary line used for hours and empty outlook copy. */
    @Composable
    private fun MetaText(text: String) {
        Text(
            text = text,
            style = TextStyle(color = secondaryColour, fontSize = META_TEXT_SIZE),
            maxLines = 1,
        )
    }

    /** Draws a score-free prompt when the cache is expired, empty, or invalid. */
    @Composable
    private fun UnavailableContent(model: WidgetUiModel.Unavailable) {
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.Start,
        ) {
            Text(
                text = model.location,
                style = TextStyle(
                    color = secondaryColour,
                    fontSize = LOCATION_TEXT_SIZE,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            Text(
                text = context.getString(R.string.widget_no_forecast),
                style = TextStyle(
                    color = primaryColour,
                    fontSize = UNAVAILABLE_TEXT_SIZE,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Text(
                text = context.getString(R.string.widget_open_app),
                style = TextStyle(
                    color = secondaryColour,
                    fontSize = META_TEXT_SIZE,
                ),
                maxLines = 1,
            )
        }
    }

    /** Resolves the phone's score band to widget-owned localized copy. */
    private fun bandLabelResource(band: TodayScoreBand): Int =
        when (band) {
            TodayScoreBand.Calm -> R.string.widget_band_calm
            TodayScoreBand.Mild -> R.string.widget_band_mild
            TodayScoreBand.Noticeable -> R.string.widget_band_noticeable
            TodayScoreBand.Rough -> R.string.widget_band_rough
            TodayScoreBand.Stormy -> R.string.widget_band_stormy
        }

    private val WIDGET_CORNER_RADIUS = 20.dp
    private val BADGE_CORNER_RADIUS = 12.dp
    private val WIDGET_PADDING = 8.dp
    private val LOCATION_TEXT_SIZE = 11.sp
    private val SCORE_TEXT_SIZE = 28.sp
    private val BAND_TEXT_SIZE = 12.sp
    private val META_TEXT_SIZE = 10.sp
    private val BADGE_TEXT_SIZE = 10.sp
    private val UNAVAILABLE_TEXT_SIZE = 18.sp
}
