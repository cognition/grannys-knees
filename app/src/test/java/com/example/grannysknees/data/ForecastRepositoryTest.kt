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

    @Test
    fun `transport advancing past the hour uses the later hour score`() {
        // Review R2 probe: start 15:59:55 Toronto, transport lands at 16:00:05.
        val start = Instant.parse("2026-11-10T20:59:55Z")
        val afterHttp = Instant.parse("2026-11-10T21:00:05Z")
        val clock = MutableClock(start)
        val repo = repository(
            transport = ClockAdvancingTransport(nominalBody, clock, afterHttp),
            clock = clock,
        )

        val state = repo.loadToday()

        assertTrue("expected Available, got $state", state is TodayUiState.Available)
        assertEquals(7, (state as TodayUiState.Available).score)
        assertEquals(afterHttp, clock.instant())
    }

    @Test
    fun `failed fetch that crosses expiry hides the index`() {
        val start = Instant.ofEpochSecond(1_794_344_400L)
        val afterFail = start.plusSeconds(1)
        val fetchedAt = start.minusSeconds(180 * 60) // exactly stale at start; expired after +1s
        val store = ForecastCacheStore(temporaryFolder.newFile("expire-cache.json"))
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
        val clock = MutableClock(start)
        val repo = repository(
            store = store,
            transport = ClockAdvancingTransport(fail = true, clock = clock, after = afterFail),
            clock = clock,
        )

        val state = repo.loadToday()

        assertTrue(state is TodayUiState.Unavailable)
        assertEquals(UnavailableReason.EXPIRED, (state as TodayUiState.Unavailable).reason)
    }

    @Test
    fun `presentFromCache after hour elapses switches the displayed score`() {
        val atSixteen = Instant.ofEpochSecond(1_794_344_400L)
        val atSeventeen = Instant.ofEpochSecond(1_794_348_000L)
        val store = ForecastCacheStore(temporaryFolder.newFile("hour-cache.json"))
        store.write(
            ForecastCacheEnvelope(
                fetchedAt = atSixteen,
                locationLabel = "Toronto, ON",
                latitude = 43.65,
                longitude = -79.38,
                timezone = "America/Toronto",
                rawBody = nominalBody,
            ),
        )
        val clock = MutableClock(atSixteen)
        val repo = repository(
            store = store,
            transport = FailingTransport(),
            clock = clock,
        )

        val first = repo.presentFromCache()
        assertTrue(first is TodayUiState.Available)
        assertEquals(7, (first as TodayUiState.Available).score)

        clock.instant = atSeventeen
        val second = repo.presentFromCache()
        assertTrue(second is TodayUiState.Available)
        assertEquals(7, (second as TodayUiState.Available).score) // 17:00 display is also 7
        // Distinct hours: 16:00 score 6.9→7 display, 17:00 score 6.8→7 display.
        // Force a clearer hour change using 18:00 (display 6).
        clock.instant = Instant.ofEpochSecond(1_794_351_600L) // 18:00 local
        val third = repo.presentFromCache()
        assertTrue(third is TodayUiState.Available)
        assertEquals(6, (third as TodayUiState.Available).score)
    }

    @Test
    fun `presentFromCache after stale then expired boundaries updates freshness`() {
        val atSixteen = Instant.ofEpochSecond(1_794_344_400L)
        val store = ForecastCacheStore(temporaryFolder.newFile("fresh-cache.json"))
        store.write(
            ForecastCacheEnvelope(
                fetchedAt = atSixteen,
                locationLabel = "Toronto, ON",
                latitude = 43.65,
                longitude = -79.38,
                timezone = "America/Toronto",
                rawBody = nominalBody,
            ),
        )
        val clock = MutableClock(atSixteen)
        val repo = repository(
            store = store,
            transport = FailingTransport(),
            clock = clock,
        )

        assertEquals(ForecastFreshness.Fresh, (repo.presentFromCache() as TodayUiState.Available).freshness)

        clock.instant = atSixteen.plusSeconds(90 * 60)
        assertEquals(ForecastFreshness.Stale, (repo.presentFromCache() as TodayUiState.Available).freshness)

        clock.instant = atSixteen.plusSeconds(181 * 60)
        val expired = repo.presentFromCache()
        assertTrue(expired is TodayUiState.Unavailable)
        assertEquals(UnavailableReason.EXPIRED, (expired as TodayUiState.Unavailable).reason)
    }

    @Test
    fun `resume style re-present uses the advanced clock`() {
        // Simulates retained Activity ON_RESUME calling presentFromCache only.
        val fetchedAt = Instant.ofEpochSecond(1_794_344_400L)
        val store = ForecastCacheStore(temporaryFolder.newFile("resume-cache.json"))
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
        val clock = MutableClock(fetchedAt)
        val repo = repository(
            store = store,
            transport = FailingTransport(),
            clock = clock,
        )
        repo.presentFromCache()

        clock.instant = fetchedAt.plusSeconds(181 * 60)
        val afterResume = repo.presentFromCache()
        assertTrue(afterResume is TodayUiState.Unavailable)
        assertEquals(UnavailableReason.EXPIRED, (afterResume as TodayUiState.Unavailable).reason)
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

    private class ClockAdvancingTransport(
        private val body: String = "",
        private val clock: MutableClock,
        private val after: Instant,
        private val fail: Boolean = false,
    ) : HttpTransport {
        override fun get(url: String): String {
            clock.instant = after
            if (fail) throw IOException("offline")
            return body
        }
    }

    private class MutableClock(
        @Volatile var instant: Instant,
    ) : Clock() {
        override fun getZone(): ZoneOffset = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId): Clock = this

        override fun instant(): Instant = instant
    }
}
