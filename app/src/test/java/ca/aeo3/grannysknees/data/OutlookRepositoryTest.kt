package ca.aeo3.grannysknees.data

import ca.aeo3.grannysknees.ui.outlook.OutlookUiState
import ca.aeo3.grannysknees.weather.FixtureSupport
import ca.aeo3.grannysknees.weather.HttpTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * [ForecastRepository.presentOutlookFromCache]: outlook follows the same
 * freshness gate as Today and is built from the cached, scored forecast.
 */
class OutlookRepositoryTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val nominalBody: String = FixtureSupport.inputBody("nominal")

    /** 16:00 America/Toronto on the nominal fixture day (the daily high). */
    private val atSixteen: Instant = Instant.ofEpochSecond(1_794_344_400L)

    @Test
    fun `fresh cache yields the local-day curve with the current hour marked`() {
        val repo = repositoryWithCache(fetchedAt = atSixteen, now = atSixteen)

        val outlook = repo.presentOutlookFromCache()

        assertTrue("expected Available, got $outlook", outlook is OutlookUiState.Available)
        val available = outlook as OutlookUiState.Available
        assertEquals(LocalDate.of(2026, 11, 10), available.today.date)
        assertTrue(
            "local day should have scored hours once the 12h window exists",
            available.today.points.isNotEmpty(),
        )
        assertTrue(available.today.points.size <= 24)
        assertEquals(atSixteen.epochSecond, available.today.currentHourEpoch)
        assertNotNull(available.today.range)
        assertEquals(OutlookMapper.UPCOMING_DAY_COUNT, available.upcoming.size)
    }

    @Test
    fun `stale cache still shows the outlook`() {
        val repo = repositoryWithCache(
            fetchedAt = atSixteen.minusSeconds(150 * 60),
            now = atSixteen,
        )

        assertTrue(repo.presentOutlookFromCache() is OutlookUiState.Available)
    }

    @Test
    fun `expired cache hides the outlook`() {
        val repo = repositoryWithCache(
            fetchedAt = atSixteen.minusSeconds(181 * 60),
            now = atSixteen,
        )

        assertEquals(OutlookUiState.Hidden, repo.presentOutlookFromCache())
    }

    @Test
    fun `empty cache hides the outlook`() {
        val repo = ForecastRepository(
            store = ForecastCacheStore(temporaryFolder.newFile("empty.json")),
            transport = FailingTransport(),
            clock = Clock.fixed(atSixteen, ZoneOffset.UTC),
        )

        assertEquals(OutlookUiState.Hidden, repo.presentOutlookFromCache())
    }

    @Test
    fun `unparseable cache hides the outlook`() {
        val repo = repositoryWithCache(fetchedAt = atSixteen, now = atSixteen, body = "{}")

        assertEquals(OutlookUiState.Hidden, repo.presentOutlookFromCache())
    }

    private fun repositoryWithCache(
        fetchedAt: Instant,
        now: Instant,
        body: String = nominalBody,
    ): ForecastRepository {
        val store = ForecastCacheStore(temporaryFolder.newFile("outlook-cache.json"))
        store.write(
            ForecastCacheEnvelope(
                fetchedAt = fetchedAt,
                locationLabel = "Toronto, ON",
                latitude = 43.65,
                longitude = -79.38,
                timezone = "America/Toronto",
                rawBody = body,
            ),
        )
        return ForecastRepository(
            store = store,
            transport = FailingTransport(),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )
    }

    private class FailingTransport : HttpTransport {
        override fun get(url: String): String = throw IOException("offline")
    }
}
