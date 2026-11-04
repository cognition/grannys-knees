package com.example.grannysknees.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.grannysknees.MainActivity
import com.example.grannysknees.R
import com.example.grannysknees.ui.today.TodayScoreBand

/** Compact home-screen widget showing the current cached knee-weather index. */
internal class GrannysKneesWidget : GlanceAppWidget() {
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
    private val backgroundColour = ColorProvider(Color(0xFFF4F7F2))
    private val primaryColour = ColorProvider(Color(0xFF17352A))
    private val secondaryColour = ColorProvider(Color(0xFF557066))
    private val badgeBackgroundColour = ColorProvider(Color(0xFFFFE7A3))
    private val badgeTextColour = ColorProvider(Color(0xFF664B00))

    /** Draws the full tappable widget surface. */
    @Composable
    fun Content(model: WidgetUiModel) {
        val context = LocalContext.current
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
                is WidgetUiModel.Available -> AvailableContent(model)
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
                Text(
                    text = model.score.toString(),
                    style = TextStyle(
                        color = primaryColour,
                        fontSize = SCORE_TEXT_SIZE,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
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
