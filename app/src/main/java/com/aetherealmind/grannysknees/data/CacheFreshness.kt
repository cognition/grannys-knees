package com.aetherealmind.grannysknees.data

import java.time.Duration
import java.time.Instant

/**
 * Contract §6 / `cache-freshness` policy defaults.
 *
 * age = referenceNow − fetchedAt:
 * - ≤ [freshFor] → [CacheStatus.FRESH]
 * - ≤ [staleUntil] → [CacheStatus.STALE] (usable, must be labelled)
 * - older → [CacheStatus.EXPIRED] (do not present index as current)
 */
internal data class CachePolicy(
    val freshFor: Duration = Duration.ofMinutes(60),
    val staleUntil: Duration = Duration.ofMinutes(180),
) {
    init {
        require(!freshFor.isNegative && !freshFor.isZero) { "freshFor must be positive" }
        require(staleUntil > freshFor) { "staleUntil must be greater than freshFor" }
    }

    companion object {
        /** Defaults from `docs/contracts/fixtures/cache-freshness.expected.json`. */
        val CONTRACT_DEFAULT: CachePolicy = CachePolicy()
    }
}

/** Derived cache usability for a stored forecast envelope. */
internal enum class CacheStatus {
    FRESH,
    STALE,
    EXPIRED,
    /** No envelope on disk. */
    UNAVAILABLE,
}

/** Evaluates cache age against [CachePolicy] at a fixed [referenceNow]. */
internal object CacheFreshness {
    fun evaluate(
        fetchedAt: Instant?,
        referenceNow: Instant,
        policy: CachePolicy = CachePolicy.CONTRACT_DEFAULT,
    ): CacheStatus {
        if (fetchedAt == null) return CacheStatus.UNAVAILABLE
        val age = Duration.between(fetchedAt, referenceNow)
        if (age.isNegative) {
            // Clock skew: treat as fresh rather than inventing expiry.
            return CacheStatus.FRESH
        }
        return when {
            age <= policy.freshFor -> CacheStatus.FRESH
            age <= policy.staleUntil -> CacheStatus.STALE
            else -> CacheStatus.EXPIRED
        }
    }
}
