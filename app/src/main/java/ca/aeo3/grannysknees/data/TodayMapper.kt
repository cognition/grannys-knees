package ca.aeo3.grannysknees.data

import ca.aeo3.grannysknees.index.HourIndex
import ca.aeo3.grannysknees.index.HourStatus
import ca.aeo3.grannysknees.index.IndexResult
import ca.aeo3.grannysknees.ui.today.ForecastFreshness
import ca.aeo3.grannysknees.ui.today.TodayUiState
import ca.aeo3.grannysknees.ui.today.UnavailableReason
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Maps scored forecast + cache freshness into [TodayUiState].
 *
 * Current-hour rule (T06 plan): convert [referenceNow] with the forecast IANA
 * zone; select the hourly row whose local truncated hour matches. Missing or
 * unavailable hour → no invented score.
 */
internal object TodayMapper {
    private val lastUpdatedFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.CANADA)

    fun toUiState(
        locationLabel: String,
        fetchedAt: Instant?,
        cacheStatus: CacheStatus,
        indexResult: IndexResult?,
        zoneId: ZoneId,
        referenceNow: Instant,
        personalResult: IndexResult? = null,
        forecastHasSnow: Boolean = false,
    ): TodayUiState {
        val lastUpdated = formatFetchedAt(fetchedAt, zoneId)
        return when (cacheStatus) {
            CacheStatus.UNAVAILABLE -> TodayUiState.Unavailable(
                location = locationLabel,
                lastUpdatedLabel = lastUpdated ?: "not yet",
                reason = UnavailableReason.EMPTY,
            )

            CacheStatus.EXPIRED -> TodayUiState.Unavailable(
                location = locationLabel,
                lastUpdatedLabel = lastUpdated ?: "not yet",
                reason = UnavailableReason.EXPIRED,
            )

            CacheStatus.FRESH, CacheStatus.STALE -> {
                if (indexResult == null) {
                    return TodayUiState.Unavailable(
                        location = locationLabel,
                        lastUpdatedLabel = lastUpdated ?: "not yet",
                        reason = UnavailableReason.EMPTY,
                    )
                }
                val hour = selectCurrentHour(indexResult.hours, zoneId, referenceNow)
                    ?: return TodayUiState.Unavailable(
                        location = locationLabel,
                        lastUpdatedLabel = lastUpdated ?: "not yet",
                        reason = UnavailableReason.INVALID_HOUR,
                    )
                if (hour.status != HourStatus.AVAILABLE || hour.indexScore == null) {
                    return TodayUiState.Unavailable(
                        location = locationLabel,
                        lastUpdatedLabel = lastUpdated ?: "not yet",
                        reason = UnavailableReason.INVALID_HOUR,
                    )
                }
                val freshness = if (cacheStatus == CacheStatus.FRESH) {
                    ForecastFreshness.Fresh
                } else {
                    ForecastFreshness.Stale
                }
                val personalHour = personalResult?.let { result ->
                    selectCurrentHour(result.hours, zoneId, referenceNow)
                }
                val personalScore = personalHour
                    ?.takeIf { it.status == HourStatus.AVAILABLE }
                    ?.indexScore
                TodayUiState.Available(
                    location = locationLabel,
                    lastUpdatedLabel = lastUpdated ?: "just now",
                    score = hour.indexScore!!,
                    freshness = freshness,
                    personalScore = personalScore,
                    forecastHasSnow = forecastHasSnow,
                )
            }
        }
    }

    fun selectCurrentHour(
        hours: List<HourIndex>,
        zoneId: ZoneId,
        referenceNow: Instant,
    ): HourIndex? {
        val hourStartEpoch = referenceNow.atZone(zoneId)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .toEpochSecond()
        return hours.firstOrNull { it.epoch == hourStartEpoch }
    }

    fun formatFetchedAt(fetchedAt: Instant?, zoneId: ZoneId): String? {
        if (fetchedAt == null) return null
        return lastUpdatedFormatter.format(fetchedAt.atZone(zoneId))
    }
}
