package ca.aeo3.grannysknees.journal

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ca.aeo3.grannysknees.data.AtomicFileWriter
import kotlinx.serialization.json.put
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * One place marked on the report figure.
 */
internal data class ChosenPlace(
    val part: String,
    val side: String?,
    val symptom: String,
    val rating: Int,
)

/**
 * Bounds a rating for [symptom].
 *
 * Ache cannot be zero — choosing it raises a clear 0 to at least 1. Stiffness
 * still allows an explicit zero.
 */
internal fun ratingForSymptom(symptom: String, rating: Int): Int {
    val bounded = rating.coerceIn(0, 10)
    return if (symptom == "ache") {
        bounded.coerceAtLeast(1)
    } else {
        bounded
    }
}

/**
 * The figure the person is filling in, and when it was last kept.
 */
internal data class ReportDummy(
    val places: List<ChosenPlace>,
    val focus: Int,
    val savedAt: Instant,
)

/**
 * The report figure belongs to a local day that starts at 3:00 a.m.
 */
internal object ReportDay {
    const val BOUNDARY_HOUR: Int = 3

    /** The reporting date for [instant]. Before 3:00 a.m. still belongs to the day before. */
    fun reportingDay(instant: Instant, zone: ZoneId): LocalDate {
        val local = instant.atZone(zone)
        val date = local.toLocalDate()
        return if (local.hour < BOUNDARY_HOUR) date.minusDays(1) else date
    }

    /** True when both instants fall in the same reporting day for [zone]. */
    fun sameReportingDay(left: Instant, right: Instant, zone: ZoneId): Boolean {
        return reportingDay(left, zone) == reportingDay(right, zone)
    }

    /**
     * The next 3:00 a.m. strictly after [instant].
     * An instant that lands on 3:00 a.m. waits until the following day.
     */
    fun nextBoundary(instant: Instant, zone: ZoneId): Instant {
        val local = instant.atZone(zone)
        var boundary = local.toLocalDate().atTime(BOUNDARY_HOUR, 0).atZone(zone)
        if (!boundary.toInstant().isAfter(instant)) {
            boundary = boundary.plusDays(1)
        }
        return boundary.toInstant()
    }
}

/** The figure shown before anything has been marked today. */
internal fun defaultReportPlaces(): List<ChosenPlace> {
    return listOf(
        ChosenPlace(
            part = "knee",
            side = "left",
            symptom = "ache",
            rating = ratingForSymptom("ache", 0),
        ),
    )
}

/**
 * Keeps the in-progress figure on this phone until the next 3:00 a.m.
 * A missing or stale file returns the starting figure. Saved reports are left alone.
 */
internal class ReportDummyStore(
    private val file: File,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    /**
     * The figure for [now]. A draft from an earlier reporting day is dropped.
     */
    fun read(now: Instant, zone: ZoneId): ReportDummy {
        val stored = readFile()
        if (stored == null || !ReportDay.sameReportingDay(stored.savedAt, now, zone)) {
            return fresh(now)
        }
        return stored
    }

    /** Replaces the stored figure. */
    fun write(dummy: ReportDummy) {
        val payload = buildJsonObject {
            put("savedAt", dummy.savedAt.truncatedTo(ChronoUnit.MILLIS).toString())
            put("focus", dummy.focus)
            put(
                "places",
                buildJsonArray {
                    dummy.places.forEach { place ->
                        add(
                            buildJsonObject {
                                put("part", place.part)
                                put("side", place.side?.let { kotlinx.serialization.json.JsonPrimitive(it) } ?: JsonNull)
                                put("symptom", place.symptom)
                                put("rating", place.rating)
                            },
                        )
                    }
                },
            )
        }.toString()
        AtomicFileWriter.writeText(file, payload)
    }

    /** The starting figure, stamped at [now]. */
    fun fresh(now: Instant): ReportDummy {
        return ReportDummy(places = defaultReportPlaces(), focus = 0, savedAt = now)
    }

    private fun readFile(): ReportDummy? {
        if (!file.exists()) {
            return null
        }
        return try {
            parse(file.readText(Charsets.UTF_8))
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: SerializationException) {
            null
        }
    }

    private fun parse(text: String): ReportDummy? {
        val root = json.parseToJsonElement(text).jsonObject
        val savedAtText = root.string("savedAt") ?: return null
        val savedAt = Instant.parse(savedAtText)
        val places = root["places"]?.jsonArray?.mapNotNull { element ->
            val item = element.jsonObject
            val part = item.string("part") ?: return@mapNotNull null
            val symptom = item.string("symptom") ?: return@mapNotNull null
            if (part.isBlank() || symptom.isBlank()) {
                return@mapNotNull null
            }
            val rating = item["rating"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val sideNode = item["side"]
            val side = when {
                sideNode == null || sideNode is JsonNull -> null
                else -> sideNode.jsonPrimitive.contentOrNull
            }
            ChosenPlace(
                part = part,
                side = side,
                symptom = symptom,
                rating = ratingForSymptom(symptom, rating),
            )
        } ?: return null
        if (places.isEmpty()) {
            return null
        }
        val focus = root["focus"]?.jsonPrimitive?.intOrNull ?: 0
        return ReportDummy(
            places = places,
            focus = focus.coerceIn(0, places.lastIndex),
            savedAt = savedAt,
        )
    }

    private fun JsonObject.string(key: String): String? {
        return this[key]?.jsonPrimitive?.contentOrNull
    }
}

/**
 * Places as they stood at the end of [day].
 * A later correction or deletion does not change an earlier day.
 */
internal fun placesAsOf(
    observations: List<JournalObservation>,
    day: LocalDate,
    zone: ZoneId,
): List<ChosenPlace> {
    val superseded = observations
        .filter { observation -> ReportDay.reportingDay(observation.observedAt, zone) <= day }
        .mapNotNull { observation -> observation.correctsObservationId }
        .toSet()
    val visible = observations
        .filter { observation ->
            observation.deletedAt == null &&
                observation.measurements.isNotEmpty() &&
                observation.id !in superseded &&
                ReportDay.reportingDay(observation.observedAt, zone) <= day
        }
        .sortedBy { observation -> observation.observedAt }
    val latest = linkedMapOf<SeriesKey, ChosenPlace>()
    visible.forEach { observation ->
        observation.measurements.forEach { measurement ->
            val key = SeriesKey(measurement.bodyPart, measurement.side, measurement.symptom)
            latest.remove(key)
            latest[key] = ChosenPlace(
                part = measurement.bodyPart,
                side = measurement.side,
                symptom = measurement.symptom,
                rating = ratingForSymptom(measurement.symptom, measurement.rating),
            )
        }
    }
    return latest.values.toList()
}

/**
 * Places from reports before today's 3:00 a.m.
 * A correction or deletion made today does not hide yesterday.
 * An empty list means there is nothing to bring back.
 */
internal fun placesFromYesterday(
    observations: List<JournalObservation>,
    now: Instant,
    zone: ZoneId,
): List<ChosenPlace> {
    val yesterday = ReportDay.reportingDay(now, zone).minusDays(1)
    return placesAsOf(observations, yesterday, zone)
}

/** One day in the past-week time-lapse. */
internal data class WeekFrame(
    val day: LocalDate,
    val label: String,
    val places: List<ChosenPlace>,
)

internal const val WEEK_LENGTH: Int = 7

/**
 * Seven reporting days, oldest first, ending today.
 * Each frame is how the pain stood at the end of that day.
 */
internal fun weekFrames(
    observations: List<JournalObservation>,
    now: Instant,
    zone: ZoneId,
    locale: Locale = Locale.CANADA,
): List<WeekFrame> {
    val today = ReportDay.reportingDay(now, zone)
    val formatter = DateTimeFormatter.ofPattern("EEE d MMM", locale)
    return (WEEK_LENGTH - 1 downTo 0).map { ago ->
        val day = today.minusDays(ago.toLong())
        WeekFrame(
            day = day,
            label = formatter.format(day),
            places = placesAsOf(observations, day, zone),
        )
    }
}
