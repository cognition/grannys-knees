package com.example.grannysknees.widget

import com.example.grannysknees.data.ForecastCacheEnvelope
import com.example.grannysknees.data.ForecastCacheStore
import com.example.grannysknees.data.ForecastRepository
import androidx.compose.ui.unit.dp
import com.example.grannysknees.ui.outlook.CurvePoint
import com.example.grannysknees.ui.outlook.DayCurve
import com.example.grannysknees.ui.outlook.OutlookDay
import com.example.grannysknees.ui.outlook.OutlookUiState
import com.example.grannysknees.ui.today.ForecastFreshness
import com.example.grannysknees.ui.today.TodayUiState
import com.example.grannysknees.ui.today.UnavailableReason
import com.example.grannysknees.weather.FixtureSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Verifies that the widget presents the same cached state as the phone. */
class WidgetModelLoaderTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val referenceNow: Instant = Instant.ofEpochSecond(1_794_344_400L)
    private val zoneId: ZoneId = ZoneId.of("America/Toronto")
    private val nominalBody: String = FixtureSupport.inputBody("nominal")

    /** Fresh widget output must preserve the phone's score and freshness. */
    @Test
    fun `fresh widget model equals phone presentation at fixed instant`() {
        val repository = repositoryWithCache(fetchedAt = referenceNow)
        val phoneState = repository.presentFromCache(referenceNow, zoneId)

        val widgetState = WidgetModelLoader(
            repository = repository,
            clock = Clock.fixed(referenceNow, ZoneOffset.UTC),
        ).load()

        assertTrue(phoneState is TodayUiState.Available)
        assertTrue(widgetState is WidgetUiModel.Available)
        phoneState as TodayUiState.Available
        widgetState as WidgetUiModel.Available
        assertEquals(phoneState.score, widgetState.score)
        assertEquals(phoneState.band, widgetState.band)
        assertEquals(phoneState.freshness, widgetState.freshness)
        assertEquals(phoneState.lastUpdatedLabel, widgetState.lastUpdatedLabel)
        assertFalse(widgetState.isStale)
        assertOutlookMatches(repository, widgetState)
    }

    /** Stale widget output must keep the phone's score and add stale state. */
    @Test
    fun `stale widget model equals phone presentation at fixed instant`() {
        val repository = repositoryWithCache(
            fetchedAt = referenceNow.minusSeconds(STALE_AGE_MINUTES * SECONDS_PER_MINUTE),
        )
        val phoneState = repository.presentFromCache(referenceNow, zoneId)

        val widgetState = WidgetModelLoader(
            repository = repository,
            clock = Clock.fixed(referenceNow, ZoneOffset.UTC),
        ).load()

        assertTrue(phoneState is TodayUiState.Available)
        assertTrue(widgetState is WidgetUiModel.Available)
        phoneState as TodayUiState.Available
        widgetState as WidgetUiModel.Available
        assertEquals(phoneState.score, widgetState.score)
        assertEquals(ForecastFreshness.Stale, phoneState.freshness)
        assertEquals(phoneState.freshness, widgetState.freshness)
        assertTrue(widgetState.isStale)
        assertOutlookMatches(repository, widgetState)
    }

    /** Expired cache must map to a score-free widget model. */
    @Test
    fun `expired widget model never exposes an index`() {
        val repository = repositoryWithCache(
            fetchedAt = referenceNow.minusSeconds(EXPIRED_AGE_MINUTES * SECONDS_PER_MINUTE),
        )

        val widgetState = WidgetModelLoader(repository).load(referenceNow, zoneId)

        assertTrue(widgetState is WidgetUiModel.Unavailable)
        assertEquals(
            UnavailableReason.EXPIRED,
            (widgetState as WidgetUiModel.Unavailable).reason,
        )
    }

    /** Empty cache must map to a score-free widget model. */
    @Test
    fun `empty widget model never exposes an index`() {
        val repository = ForecastRepository(
            store = ForecastCacheStore(temporaryFolder.newFile("empty-cache.json")),
        )

        val widgetState = WidgetModelLoader(repository).load(referenceNow, zoneId)

        assertTrue(widgetState is WidgetUiModel.Unavailable)
        assertEquals(
            UnavailableReason.EMPTY,
            (widgetState as WidgetUiModel.Unavailable).reason,
        )
    }

    /** A wide cell lists hours. A tall cell lists the five days. A small cell does neither. */
    @Test
    fun `layout picker follows width then height`() {
        assertEquals(
            WidgetLayout.Current,
            WidgetLayoutPicker.pick(width = 168.dp, height = 72.dp),
        )
        assertEquals(
            WidgetLayout.Hourly,
            WidgetLayoutPicker.pick(width = 280.dp, height = 160.dp),
        )
        assertEquals(
            WidgetLayout.FiveDay,
            WidgetLayoutPicker.pick(width = 280.dp, height = 280.dp),
        )
    }

    /** The short widget keeps morning and evening hours, plus a mark for now. */
    @Test
    fun `hourly model keeps the whole day and the current hour`() {
        val points = listOf(
            CurvePoint(epoch = 0L, timeLabel = "00:00", utcOffsetSeconds = 0, score = BigDecimal("1.0")),
            CurvePoint(
                epoch = 18 * 3_600L,
                timeLabel = "18:00",
                utcOffsetSeconds = 0,
                score = BigDecimal("4.0"),
            ),
        )
        val outlook = OutlookUiState.Available(
            today = DayCurve(
                date = LocalDate.parse("2026-11-04"),
                startEpoch = 0L,
                endEpoch = 86_400L,
                segments = listOf(points),
                axisTicks = emptyList(),
                nowFraction = 18f / 24f,
                currentHourEpoch = 18 * 3_600L,
                range = null,
            ),
            upcoming = emptyList(),
        )
        val today = TodayUiState.Available(
            location = "London, Ontario",
            lastUpdatedLabel = "Nov 4, 6:00 p.m.",
            score = 4,
            freshness = ForecastFreshness.Fresh,
        )

        val model = WidgetModelMapper().from(today, outlook) as WidgetUiModel.Available

        assertEquals(listOf("00:00", "18:00"), model.hours.map { hour -> hour.timeLabel })
        assertEquals(18f / 24f, model.nowFraction)
    }

    /** A day Today left unscored stays null on the widget. It is not stored as zero. */
    @Test
    fun `mapper keeps a missing day null rather than zero`() {
        val today = TodayUiState.Available(
            location = "Toronto, ON",
            lastUpdatedLabel = "Nov 4, 6:35 p.m.",
            score = 4,
            freshness = ForecastFreshness.Fresh,
        )
        val outlook = OutlookUiState.Available(
            today = DayCurve(
                date = LocalDate.parse("2026-11-04"),
                startEpoch = 0L,
                endEpoch = 86_400L,
                segments = emptyList(),
                axisTicks = emptyList(),
                nowFraction = null,
                currentHourEpoch = null,
                range = null,
            ),
            upcoming = listOf(
                OutlookDay(
                    date = LocalDate.parse("2026-11-05"),
                    dayLabel = "Thu 5",
                    range = null,
                ),
            ),
        )

        val model = WidgetModelMapper().from(today, outlook)

        assertTrue(model is WidgetUiModel.Available)
        model as WidgetUiModel.Available
        assertTrue(model.hours.isEmpty())
        assertEquals("Thu 5", model.days.single().dayLabel)
        assertNull(model.days.single().low)
        assertNull(model.days.single().high)
    }

    /** Hourly labels and five-day extrema must be the outlook's, including null days. */
    private fun assertOutlookMatches(
        repository: ForecastRepository,
        widgetState: WidgetUiModel.Available,
    ) {
        val outlook = repository.presentOutlookFromCache(referenceNow)
        assertTrue(outlook is OutlookUiState.Available)
        outlook as OutlookUiState.Available
        assertEquals(
            outlook.today.points.map { it.timeLabel to it.score },
            widgetState.hours.map { it.timeLabel to it.score },
        )
        assertEquals(outlook.upcoming.size, widgetState.days.size)
        outlook.upcoming.zip(widgetState.days).forEach { (expected, actual) ->
            assertEquals(expected.dayLabel, actual.dayLabel)
            assertEquals(expected.range?.low, actual.low)
            assertEquals(expected.range?.high, actual.high)
            if (expected.range == null) {
                assertNull(actual.low)
                assertNull(actual.high)
            }
        }
    }

    /** Creates a repository backed by a canonical forecast fixture. */
    private fun repositoryWithCache(fetchedAt: Instant): ForecastRepository {
        val store = ForecastCacheStore(temporaryFolder.newFile("widget-cache.json"))
        store.write(
            ForecastCacheEnvelope(
                fetchedAt = fetchedAt,
                locationLabel = "Toronto, ON",
                latitude = 43.65,
                longitude = -79.38,
                timezone = "America/Toronto",
                rawBody = nominalBody,
            ),
        )
        return ForecastRepository(store = store)
    }

    private companion object {
        const val SECONDS_PER_MINUTE: Long = 60L
        const val STALE_AGE_MINUTES: Long = 90L
        const val EXPIRED_AGE_MINUTES: Long = 181L
    }
}
