package com.example.grannysknees.journal

import java.time.Instant

/**
 * Append-only journal rules from symptom-observations/1.0.0.
 *
 * Untouched series are omitted. An explicit zero is stored. A confirmation
 * copies the previous rating and does not rewrite the older row. An empty
 * save writes nothing.
 */
internal object JournalCommitter {
    const val CHANGED: String = "changed"
    const val CONFIRMED_UNCHANGED: String = "confirmed_unchanged"

    /**
     * Builds the next observation, or null when nothing changed and no
     * confirmation had a previous rating.
     */
    fun commit(
        committedAt: Instant,
        prior: List<JournalObservation>,
        edits: List<SeriesEdit>,
        confirmations: List<SeriesKey>,
        newId: String,
        sensorReadings: List<SensorReadingDraft> = emptyList(),
        snapshotId: String? = null,
    ): JournalCommit {
        val measurements = mutableListOf<JournalMeasurement>()
        edits.forEach { edit ->
            val previous = latestMeasurement(prior, edit.bodyPart, edit.side, edit.symptom)
            measurements += JournalMeasurement(
                bodyPart = edit.bodyPart,
                side = edit.side,
                symptom = edit.symptom,
                rating = edit.rating,
                reportKind = CHANGED,
                previousObservationId = previous?.first?.id,
                previousRating = previous?.second?.rating,
                previousObservedAt = previous?.first?.observedAt,
            )
        }
        confirmations.forEach { key ->
            val previous = latestMeasurement(prior, key.bodyPart, key.side, key.symptom)
                ?: return@forEach
            measurements += JournalMeasurement(
                bodyPart = key.bodyPart,
                side = key.side,
                symptom = key.symptom,
                rating = previous.second.rating,
                reportKind = CONFIRMED_UNCHANGED,
                previousObservationId = previous.first.id,
                previousRating = previous.second.rating,
                previousObservedAt = previous.first.observedAt,
            )
        }
        if (measurements.isEmpty()) {
            return JournalCommit(observation = null, snapshot = null)
        }
        val observation = JournalObservation(
            id = newId,
            observedAt = committedAt,
            correctsObservationId = null,
            deletedAt = null,
            measurements = measurements,
        )
        val snapshot = if (snapshotId == null) {
            null
        } else {
            JournalSnapshot(
                id = snapshotId,
                observationId = newId,
                capturedAt = committedAt,
                readings = sensorReadings,
            )
        }
        return JournalCommit(observation = observation, snapshot = snapshot)
    }

    /** Tombstone for one report. The original row stays in [prior]. */
    fun delete(
        targetId: String,
        deletedAt: Instant,
        newId: String,
    ): JournalObservation = JournalObservation(
        id = newId,
        observedAt = deletedAt,
        correctsObservationId = targetId,
        deletedAt = deletedAt,
        measurements = emptyList(),
    )

    /**
     * Removes one measurement from [target] without wiping the rest of the set.
     *
     * The last remaining measurement becomes a tombstone. Otherwise a
     * correction keeps the other rows and preserves their displayed times.
     * Returns null when [target] does not contain that series.
     */
    fun removeMeasurement(
        target: JournalObservation,
        bodyPart: String,
        side: String?,
        symptom: String,
        deletedAt: Instant,
        newId: String,
    ): JournalObservation? {
        val remaining = target.measurements.filterNot { measurement ->
            measurement.bodyPart == bodyPart &&
                measurement.side == side &&
                measurement.symptom == symptom
        }
        if (remaining.size == target.measurements.size) {
            return null
        }
        if (remaining.isEmpty()) {
            return delete(
                targetId = target.id,
                deletedAt = deletedAt,
                newId = newId,
            )
        }
        return JournalObservation(
            id = newId,
            observedAt = deletedAt,
            correctsObservationId = target.id,
            deletedAt = null,
            measurements = remaining.map { measurement ->
                measurement.copy(
                    previousObservedAt = measurement.previousObservedAt ?: target.observedAt,
                )
            },
        )
    }

    /**
     * Reports that are still current: not a tombstone, and not the target of
     * a later correction or deletion.
     */
    fun current(observations: List<JournalObservation>): List<JournalObservation> {
        val superseded = observations.mapNotNull { observation -> observation.correctsObservationId }.toSet()
        return observations.filter { observation ->
            observation.deletedAt == null && observation.id !in superseded
        }
    }

    /** Latest current measurement for a series, without rewriting history. */
    fun latestMeasurement(
        observations: List<JournalObservation>,
        bodyPart: String,
        side: String?,
        symptom: String,
    ): Pair<JournalObservation, JournalMeasurement>? {
        return current(observations)
            .sortedBy { observation -> observation.observedAt }
            .mapNotNull { observation ->
                val measurement = observation.measurements.firstOrNull { item ->
                    item.bodyPart == bodyPart && item.side == side && item.symptom == symptom
                }
                if (measurement == null) null else observation to measurement
            }
            .lastOrNull()
    }
}
