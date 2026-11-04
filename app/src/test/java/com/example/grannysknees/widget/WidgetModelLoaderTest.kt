package com.example.grannysknees.widget

import com.example.grannysknees.data.ForecastCacheEnvelope
import com.example.grannysknees.data.ForecastCacheStore
import com.example.grannysknees.data.ForecastRepository
import com.example.grannysknees.ui.today.ForecastFreshness
import com.example.grannysknees.ui.today.TodayUiState
import com.example.grannysknees.ui.today.UnavailableReason
import com.example.grannysknees.weather.FixtureSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Clock
import java.time.Instant
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
