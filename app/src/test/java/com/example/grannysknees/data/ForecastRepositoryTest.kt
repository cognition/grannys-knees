package com.example.grannysknees.data

import com.example.grannysknees.ui.today.ForecastFreshness
import com.example.grannysknees.ui.today.TodayUiState
import com.example.grannysknees.ui.today.UnavailableReason
import com.example.grannysknees.weather.FixtureSupport
import com.example.grannysknees.weather.HttpTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Repository behaviour with fixture payloads and a controllable clock/transport.
 */
class ForecastRepositoryTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val nominalBody: String = FixtureSupport.inputBody("nominal")

    private val missingBody: String = FixtureSupport.inputBody("missing-values")

    @Test
    fun `successful fetch presents a live index for the current hour`() {
        // nominal.input covers 2026-11-09 evening through 2026-11-10; pick 16:00 local
        // (epoch 1794344400) which is uniquely the daily high in the fixture.
        val now = Instant.ofEpochSecond(1_794_344_400L)
        val repo = repository(
            transport = FixedTransport(nominalBody),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

        val state = repo.loadToday()

        assertTrue(state is TodayUiState.Available)
        val available = state as TodayUiState.Available
        assertEquals(ForecastFreshness.Fresh, available.freshness)
        assertEquals("Toronto, ON", available.location)
        assertTrue(available.score in 0..10)
    }

    @Test
    fun `network failure still shows stale cache when age allows`() {
        // 16:00 America/Toronto on the nominal day — an available scored hour.
        val referenceNow = Instant.ofEpochSecond(1_794_344_400L)
        val fetchedAt = referenceNow.minusSeconds(150 * 60) // 150 min → stale
        val store = ForecastCacheStore(temporaryFolder.newFile("cache.json"))
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
        val repo = repository(
            store = store,
            transport = FailingTransport(),
            clock = Clock.fixed(referenceNow, ZoneOffset.UTC),
        )

        val state = repo.loadToday()

        assertTrue("expected Available, got $state", state is TodayUiState.Available)
        assertEquals(ForecastFreshness.Stale, (state as TodayUiState.Available).freshness)
    }

    @Test
    fun `expired cache hides the index`() {
        val referenceNow = Instant.ofEpochSecond(1_794_344_400L)
        val fetchedAt = referenceNow.minusSeconds(245 * 60) // 245 min → expired
        val store = ForecastCacheStore(temporaryFolder.newFile("cache.json"))
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
        val repo = repository(
            store = store,
            transport = FailingTransport(),
            clock = Clock.fixed(referenceNow, ZoneOffset.UTC),
        )

        val state = repo.loadToday()

        assertTrue(state is TodayUiState.Unavailable)
        assertEquals(UnavailableReason.EXPIRED, (state as TodayUiState.Unavailable).reason)
    }

    @Test
    fun `empty cache offline is unavailable empty`() {
        val repo = repository(
            transport = FailingTransport(),
            clock = Clock.fixed(Instant.parse("2026-11-10T04:10:00Z"), ZoneOffset.UTC),
        )

        val state = repo.loadToday()

        assertTrue(state is TodayUiState.Unavailable)
        assertEquals(UnavailableReason.EMPTY, (state as TodayUiState.Unavailable).reason)
    }

    @Test
    fun `unavailable current hour does not invent a score`() {
        // Use missing-values fixture; pick an hour known unavailable in that set.
        // 06:00 local on 2026-11-10 is epoch 1794308400 in America/Toronto (EST).
        val now = Instant.ofEpochSecond(1_794_308_400L)
        val repo = repository(
            transport = FixedTransport(missingBody),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

        val state = repo.loadToday()

        assertTrue(
            "expected Unavailable, got $state",
            state is TodayUiState.Unavailable,
        )
        assertEquals(
            UnavailableReason.INVALID_HOUR,
            (state as TodayUiState.Unavailable).reason,
        )
    }

    private fun repository(
        transport: HttpTransport,
        clock: Clock,
        store: ForecastCacheStore = ForecastCacheStore(temporaryFolder.newFile("repo-cache.json")),
    ): ForecastRepository =
        ForecastRepository(
            store = store,
            transport = transport,
            clock = clock,
        )

    private class FixedTransport(private val body: String) : HttpTransport {
        override fun get(url: String): String = body
    }

    private class FailingTransport : HttpTransport {
        override fun get(url: String): String = throw IOException("offline")
    }
}
