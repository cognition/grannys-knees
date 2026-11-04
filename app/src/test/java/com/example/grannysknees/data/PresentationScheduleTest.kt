package com.example.grannysknees.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class PresentationScheduleTest {
    private val toronto: ZoneId = ZoneId.of("America/Toronto")

    @Test
    fun `waits until the next local hour when no cache`() {
        // 15:50 Toronto EST — next hour is 10 minutes away (under the 15-minute cap).
        val now = Instant.ofEpochSecond(1_794_343_800L) // 2026-11-10T20:50:00Z
        val delay = PresentationSchedule.delayUntilNextTransition(
            now = now,
            fetchedAt = null,
            zoneId = toronto,
        )
        assertEquals(Duration.ofMinutes(10), delay)
    }

    @Test
    fun `freshness boundary can beat the next hour`() {
        val now = Instant.ofEpochSecond(1_794_344_400L) // 16:00 local
        // Stale flips at fetchedAt+60m+1ns ≈ now + 1m + 1ns → MIN_WAIT coercion to 1s
        // would hide the intent; keep 59m age so wake is ~1m+1ns (uncapped).
        val fetchedAt = now.minusSeconds(59 * 60)
        val delay = PresentationSchedule.delayUntilNextTransition(
            now = now,
            fetchedAt = fetchedAt,
            zoneId = toronto,
        )
        assertEquals(Duration.ofMinutes(1).plusNanos(1), delay)
    }

    @Test
    fun `at exact fresh threshold schedules immediate follow-up not the 15m cap`() {
        val fetchedAt = Instant.ofEpochSecond(1_794_344_400L)
        val atExactFresh = fetchedAt.plus(Duration.ofMinutes(60))
        val delay = PresentationSchedule.delayUntilNextTransition(
            now = atExactFresh,
            fetchedAt = fetchedAt,
            zoneId = toronto,
        )
        assertEquals(PresentationSchedule.MIN_WAIT, delay)
        assertTrue(delay < PresentationSchedule.MAX_WAIT)
    }

    @Test
    fun `caps long waits at fifteen minutes`() {
        val now = Instant.ofEpochSecond(1_794_340_800L) // 15:00 local
        val fetchedAt = now // post-threshold wake ~60m+1ns; next hour 60m → capped
        val delay = PresentationSchedule.delayUntilNextTransition(
            now = now,
            fetchedAt = fetchedAt,
            zoneId = toronto,
        )
        assertEquals(Duration.ofMinutes(15), delay)
    }
}
