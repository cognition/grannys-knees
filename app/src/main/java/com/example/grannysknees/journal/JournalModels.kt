package com.example.grannysknees.journal

import java.time.Instant

/** One series the user edited or confirmed. Side is null when the part has no side. */
internal data class SeriesKey(
    val bodyPart: String,
    val side: String?,
    val symptom: String,
)

/** A rating the user set, including an explicit zero. */
internal data class SeriesEdit(
    val bodyPart: String,
    val side: String?,
    val symptom: String,
    val rating: Int,
)

/** One stored measurement. Omitted series are not represented at all. */
internal data class JournalMeasurement(
    val bodyPart: String,
    val side: String?,
    val symptom: String,
    val rating: Int,
    val reportKind: String,
    val previousObservationId: String?,
    val previousRating: Int?,
    val previousObservedAt: Instant?,
)

/** One append-only observation. A deletion marker has [deletedAt] set and no measurements. */
internal data class JournalObservation(
    val id: String,
    val observedAt: Instant,
    val correctsObservationId: String?,
    val deletedAt: Instant?,
    val measurements: List<JournalMeasurement>,
)

/** A sensor reading. [value] is null exactly when [state] is unavailable. */
internal data class SensorReadingDraft(
    val quantity: String,
    val state: String,
    val value: Double?,
    val unit: String?,
    val source: String,
    val capturedAt: Instant,
)

/** Snapshot written only when an observation row is written. */
internal data class JournalSnapshot(
    val id: String,
    val observationId: String,
    val capturedAt: Instant,
    val readings: List<SensorReadingDraft>,
)

/** What one save appended. Both are null when the form had nothing to record. */
internal data class JournalCommit(
    val observation: JournalObservation?,
    val snapshot: JournalSnapshot?,
)
