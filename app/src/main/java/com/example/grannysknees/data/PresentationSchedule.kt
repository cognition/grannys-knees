package com.example.grannysknees.data

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Computes when Today must re-present cached forecast data without a new
 * network fetch: local hour rollover and §6 freshness boundary crossings.
 */
internal object PresentationSchedule {
    private val MAX_WAIT: Duration = Duration.ofMinutes(15)
    private val MIN_WAIT: Duration = Duration.ofSeconds(1)

    /**
     * Delay until the next presentation change at or after [now].
     *
     * Always returns a positive duration capped at [MAX_WAIT] so a live
     * coroutine keeps ticking even if scheduling math is sparse.
     */
    fun delayUntilNextTransition(
        now: Instant,
        fetchedAt: Instant?,
        zoneId: ZoneId,
        policy: CachePolicy = CachePolicy.CONTRACT_DEFAULT,
    ): Duration {
        val candidates = mutableListOf<Instant>()

        val nextHour = now.atZone(zoneId)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .plusHours(1)
            .toInstant()
        candidates.add(nextHour)

        if (fetchedAt != null) {
            candidates.add(fetchedAt.plus(policy.freshFor))
            candidates.add(fetchedAt.plus(policy.staleUntil))
        }

        val soonest = candidates
            .filter { it.isAfter(now) }
            .minOrNull()
            ?: now.plus(MAX_WAIT)

        val wait = Duration.between(now, soonest)
        return when {
            wait < MIN_WAIT -> MIN_WAIT
            wait > MAX_WAIT -> MAX_WAIT
            else -> wait
        }
    }
}
