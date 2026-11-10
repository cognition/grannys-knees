package ca.aeo3.grannysknees.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * Drives all four `cache-freshness.expected.json` scenarios against a fixed clock.
 */
class CacheFreshnessTest {
    private val fetchedAt: Instant = Instant.parse("2026-11-10T00:05:00Z")

    @Test
    fun `fresh within 60 minutes`() {
        val status = CacheFreshness.evaluate(
            fetchedAt = fetchedAt,
            referenceNow = Instant.parse("2026-11-10T00:30:00Z"),
        )
        assertEquals(CacheStatus.FRESH, status)
    }

    @Test
    fun `stale between 60 and 180 minutes`() {
        val status = CacheFreshness.evaluate(
            fetchedAt = fetchedAt,
            referenceNow = Instant.parse("2026-11-10T02:35:00Z"),
        )
        assertEquals(CacheStatus.STALE, status)
    }

    @Test
    fun `expired after 180 minutes`() {
        val status = CacheFreshness.evaluate(
            fetchedAt = fetchedAt,
            referenceNow = Instant.parse("2026-11-10T04:10:00Z"),
        )
        assertEquals(CacheStatus.EXPIRED, status)
    }

    @Test
    fun `unavailable when nothing was fetched`() {
        val status = CacheFreshness.evaluate(
            fetchedAt = null,
            referenceNow = Instant.parse("2026-11-10T04:10:00Z"),
        )
        assertEquals(CacheStatus.UNAVAILABLE, status)
    }
}
