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
import java.time.Duration
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
    fun `a saved city does not present another city's cached score`() {
        val now = Instant.ofEpochSecond(1_794_344_400L)
        val cache = ForecastCacheStore(temporaryFolder.newFile("toronto.json"))
        cache.write(
            ForecastCacheEnvelope(
                fetchedAt = now,
                locationLabel = "Toronto, ON",
                latitude = 43.65,
                longitude = -79.38,
                timezone = "America/Toronto",
                rawBody = nominalBody,
            ),
        )
        val cities = SelectedCityStore(temporaryFolder.newFile("city.json"))
        cities.write(
            com.example.grannysknees.data.SelectedCity(
                label = "Ottawa, Ontario",
                latitude = 45.4215,
                longitude = -75.6972,
                timezone = "America/Toronto",
                admin1 = "Ontario",
                countryCode = "CA",
            ),
        )
        val seen = mutableListOf<String>()
        val repo = ForecastRepository(
            store = cache,
            transport = HttpTransport { url ->
                seen.add(url)
                nominalBody
            },
            clock = Clock.fixed(now, ZoneOffset.UTC),
            cityStore = cities,
        )

        val beforeRefresh = repo.presentFromCache(now)
        assertTrue(beforeRefresh is TodayUiState.Unavailable)
        assertEquals("Ottawa, Ontario", beforeRefresh.location)

        val loaded = repo.loadToday()
        assertTrue(loaded is TodayUiState.Available)
        assertEquals("Ottawa, Ontario", loaded.location)
        assertTrue(seen.single().contains("latitude=45.4215"))
        assertTrue(seen.single().contains("longitude=-75.6972"))
    }

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
        assertTrue(available.score.compareTo(java.math.BigDecimal.ZERO) >= 0)
        assertTrue(available.score.compareTo(java.math.BigDecimal.TEN) <= 0)
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
        val availableScore = (state as TodayUiState.Available).score
        assertTrue(
            "score should be on the 0–10 display scale",
            availableScore.compareTo(java.math.BigDecimal.ZERO) >= 0 &&
                availableScore.compareTo(java.math.BigDecimal.TEN) <= 0,
        )
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
        assertTrue((first as TodayUiState.Available).score.compareTo(java.math.BigDecimal.ZERO) >= 0)
        assertTrue(first.score.compareTo(java.math.BigDecimal.TEN) <= 0)

        clock.instant = atSeventeen
        val second = repo.presentFromCache()
        assertTrue(second is TodayUiState.Available)
        assertTrue((second as TodayUiState.Available).score.compareTo(java.math.BigDecimal.ZERO) >= 0)
        assertTrue(second.score.compareTo(java.math.BigDecimal.TEN) <= 0)

        clock.instant = Instant.ofEpochSecond(1_794_351_600L) // 18:00 local
        val third = repo.presentFromCache()
        assertTrue(third is TodayUiState.Available)
        assertTrue((third as TodayUiState.Available).score.compareTo(java.math.BigDecimal.ZERO) >= 0)
        assertTrue(third.score.compareTo(java.math.BigDecimal.TEN) <= 0)
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
    fun `schedule plus present flips Fresh to Stale at 60m plus epsilon without 15m cap`() {
        val fetchedAt = Instant.ofEpochSecond(1_794_344_400L)
        val store = ForecastCacheStore(temporaryFolder.newFile("boundary-fresh.json"))
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
        val atExact = fetchedAt.plus(Duration.ofMinutes(60))
        val clock = MutableClock(atExact)
        val repo = repository(
            store = store,
            transport = FailingTransport(),
            clock = clock,
        )

        val stillFresh = repo.presentFromCache()
        assertTrue(stillFresh is TodayUiState.Available)
        assertEquals(ForecastFreshness.Fresh, (stillFresh as TodayUiState.Available).freshness)

        val delay = repo.delayUntilNextPresentationChange(atExact)
        assertTrue(
            "expected short wake after inclusive threshold, got $delay",
            delay < Duration.ofMinutes(15),
        )
        assertEquals(Duration.ofSeconds(1), delay) // MIN_WAIT after +1ns candidate

        clock.instant = atExact.plus(delay)
        val stale = repo.presentFromCache()
        assertTrue(stale is TodayUiState.Available)
        assertEquals(ForecastFreshness.Stale, (stale as TodayUiState.Available).freshness)
    }

    @Test
    fun `schedule plus present flips Stale to Expired at 180m plus epsilon without 15m cap`() {
        val fetchedAt = Instant.ofEpochSecond(1_794_344_400L)
        val store = ForecastCacheStore(temporaryFolder.newFile("boundary-stale.json"))
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
        val atExact = fetchedAt.plus(Duration.ofMinutes(180))
        val clock = MutableClock(atExact)
        val repo = repository(
            store = store,
            transport = FailingTransport(),
            clock = clock,
        )

        val stillStale = repo.presentFromCache()
        assertTrue(stillStale is TodayUiState.Available)
        assertEquals(ForecastFreshness.Stale, (stillStale as TodayUiState.Available).freshness)

        val delay = repo.delayUntilNextPresentationChange(atExact)
        assertTrue(delay < Duration.ofMinutes(15))
        assertEquals(Duration.ofSeconds(1), delay)

        clock.instant = atExact.plus(delay)
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

    @Test
    fun `same local day open without force skips network when stamp and cache match`() {
        val now = Instant.ofEpochSecond(1_794_344_400L)
        val store = ForecastCacheStore(temporaryFolder.newFile("day-cache.json"))
        store.write(
            ForecastCacheEnvelope(
                fetchedAt = now,
                locationLabel = "Toronto, ON",
                latitude = 43.65,
                longitude = -79.38,
                timezone = "America/Toronto",
                rawBody = nominalBody,
            ),
        )
        val stamp = DailyFetchStampStore(temporaryFolder.newFile("day-stamp.txt"))
        stamp.writeLocalDate(java.time.LocalDate.of(2026, 11, 10))
        val seen = mutableListOf<String>()
        val repo = ForecastRepository(
            store = store,
            transport = HttpTransport { url ->
                seen.add(url)
                nominalBody
            },
            clock = Clock.fixed(now, ZoneOffset.UTC),
            fetchStampStore = stamp,
        )

        val state = repo.loadToday(forceNetwork = false)

        assertTrue(state is TodayUiState.Available)
        assertTrue(seen.isEmpty())
    }

    @Test
    fun `force network still fetches on a stamped day`() {
        val now = Instant.ofEpochSecond(1_794_344_400L)
        val store = ForecastCacheStore(temporaryFolder.newFile("force-cache.json"))
        store.write(
            ForecastCacheEnvelope(
                fetchedAt = now,
                locationLabel = "Toronto, ON",
                latitude = 43.65,
                longitude = -79.38,
                timezone = "America/Toronto",
                rawBody = nominalBody,
            ),
        )
        val stamp = DailyFetchStampStore(temporaryFolder.newFile("force-stamp.txt"))
        stamp.writeLocalDate(java.time.LocalDate.of(2026, 11, 10))
        val seen = mutableListOf<String>()
        val repo = ForecastRepository(
            store = store,
            transport = HttpTransport { url ->
                seen.add(url)
                nominalBody
            },
            clock = Clock.fixed(now, ZoneOffset.UTC),
            fetchStampStore = stamp,
        )

        val state = repo.loadToday(forceNetwork = true)

        assertTrue(state is TodayUiState.Available)
        assertEquals(1, seen.size)
    }

    @Test
    fun `first open of a new local day fetches and stamps`() {
        val now = Instant.ofEpochSecond(1_794_344_400L)
        val store = ForecastCacheStore(temporaryFolder.newFile("new-day-cache.json"))
        store.write(
            ForecastCacheEnvelope(
                fetchedAt = now.minusSeconds(86_400),
                locationLabel = "Toronto, ON",
                latitude = 43.65,
                longitude = -79.38,
                timezone = "America/Toronto",
                rawBody = nominalBody,
            ),
        )
        val stamp = DailyFetchStampStore(temporaryFolder.newFile("new-day-stamp.txt"))
        stamp.writeLocalDate(java.time.LocalDate.of(2026, 11, 9))
        val seen = mutableListOf<String>()
        val repo = ForecastRepository(
            store = store,
            transport = HttpTransport { url ->
                seen.add(url)
                nominalBody
            },
            clock = Clock.fixed(now, ZoneOffset.UTC),
            fetchStampStore = stamp,
        )

        val state = repo.loadToday(forceNetwork = false)

        assertTrue(state is TodayUiState.Available)
        assertEquals(1, seen.size)
        assertEquals(java.time.LocalDate.of(2026, 11, 10), stamp.readLocalDate())
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
