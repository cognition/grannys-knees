package com.aetherealmind.grannysknees.data

import com.aetherealmind.grannysknees.index.HourIndex
import com.aetherealmind.grannysknees.index.HourInputs
import com.aetherealmind.grannysknees.index.HourStatus
import com.aetherealmind.grannysknees.index.IndexResult
import com.aetherealmind.grannysknees.index.SubScores
import com.aetherealmind.grannysknees.ui.today.ForecastFreshness
import com.aetherealmind.grannysknees.ui.today.TodayUiState
import com.aetherealmind.grannysknees.ui.today.UnavailableReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId

/**
 * UE-07 / PE-03: freshness mapping must not keep a last-good score after expiry
 * or invent a score for an unscored hour. Clock is fixed to the fixture day.
 */
class TodayMapperFreshnessTest {
    private val zone: ZoneId = ZoneId.of("America/Toronto")
    private val location: String = "Toronto, ON"
    /** Local 2026-11-10 12:00 America/Toronto (EST, UTC−5). */
    private val noonLocal: Instant = Instant.parse("2026-11-10T17:00:00Z")
    private val noonEpoch: Long = noonLocal.epochSecond
    private val fetchedAt: Instant = Instant.parse("2026-11-10T16:30:00Z")

    @Test
    fun `fresh cache with available hour shows score without stale`() {
        val state = TodayMapper.toUiState(
            locationLabel = location,
            fetchedAt = fetchedAt,
            cacheStatus = CacheStatus.FRESH,
            indexResult = indexWithHour(noonEpoch, available = true, score = "4.2"),
            zoneId = zone,
            referenceNow = noonLocal,
        )

        assertTrue(state is TodayUiState.Available)
        val available = state as TodayUiState.Available
        assertEquals(BigDecimal("4.2"), available.score)
        assertEquals(ForecastFreshness.Fresh, available.freshness)
        assertFalse(available.isStale)
    }

    @Test
    fun `stale cache keeps score and marks stale`() {
        val state = TodayMapper.toUiState(
            locationLabel = location,
            fetchedAt = fetchedAt,
            cacheStatus = CacheStatus.STALE,
            indexResult = indexWithHour(noonEpoch, available = true, score = "4.2"),
            zoneId = zone,
            referenceNow = noonLocal,
        )

        assertTrue(state is TodayUiState.Available)
        val available = state as TodayUiState.Available
        assertEquals(BigDecimal("4.2"), available.score)
        assertTrue(available.isStale)
    }

    @Test
    fun `expired cache hides score even when hours contain a value`() {
        val state = TodayMapper.toUiState(
            locationLabel = location,
            fetchedAt = fetchedAt,
            cacheStatus = CacheStatus.EXPIRED,
            indexResult = indexWithHour(noonEpoch, available = true, score = "9.9"),
            zoneId = zone,
            referenceNow = noonLocal,
        )

        assertTrue(state is TodayUiState.Unavailable)
        val unavailable = state as TodayUiState.Unavailable
        assertEquals(UnavailableReason.EXPIRED, unavailable.reason)
    }

    @Test
    fun `unavailable current hour does not invent a score from other hours`() {
        val otherHour = noonEpoch - 3_600L
        val state = TodayMapper.toUiState(
            locationLabel = location,
            fetchedAt = fetchedAt,
            cacheStatus = CacheStatus.FRESH,
            indexResult = IndexResult(
                contractVersion = "test",
                indexVersion = "test",
                hours = listOf(
                    hour(epoch = otherHour, available = true, score = "8.0"),
                    hour(epoch = noonEpoch, available = false, score = "0.0"),
                ),
                daily = emptyList(),
            ),
            zoneId = zone,
            referenceNow = noonLocal,
        )

        assertTrue(state is TodayUiState.Unavailable)
        assertEquals(
            UnavailableReason.INVALID_HOUR,
            (state as TodayUiState.Unavailable).reason,
        )
    }

    @Test
    fun `empty cache is unavailable empty not a zero score`() {
        val state = TodayMapper.toUiState(
            locationLabel = location,
            fetchedAt = null,
            cacheStatus = CacheStatus.UNAVAILABLE,
            indexResult = null,
            zoneId = zone,
            referenceNow = noonLocal,
        )

        assertTrue(state is TodayUiState.Unavailable)
        assertEquals(UnavailableReason.EMPTY, (state as TodayUiState.Unavailable).reason)
    }

    private fun indexWithHour(
        epoch: Long,
        available: Boolean,
        score: String,
    ): IndexResult = IndexResult(
        contractVersion = "test",
        indexVersion = "test",
        hours = listOf(hour(epoch = epoch, available = available, score = score)),
        daily = emptyList(),
    )

    private fun hour(
        epoch: Long,
        available: Boolean,
        score: String,
    ): HourIndex = HourIndex(
        epoch = epoch,
        instantUtc = Instant.ofEpochSecond(epoch).toString(),
        local = "2026-11-10T12:00:00-05:00",
        utcOffsetSeconds = -18_000,
        localDate = "2026-11-10",
        inputs = HourInputs(null, null, null),
        pressureTrend3hHpa = if (available) BigDecimal("-1.00") else null,
        subScores = SubScores(
            pressure = if (available) BigDecimal("-1.00") else null,
            humidity = if (available) BigDecimal("0.50") else null,
            temperature = if (available) BigDecimal("-0.25") else null,
        ),
        indexRaw = if (available) BigDecimal(score) else null,
        indexScore = if (available) BigDecimal(score) else null,
        indexDisplay = if (available) score.toDouble().toInt() else null,
        status = if (available) HourStatus.AVAILABLE else HourStatus.UNAVAILABLE,
        unavailableReasons = if (available) emptyList() else listOf("missing"),
    )
}
