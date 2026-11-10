package com.example.grannysknees.journal

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

/**
 * Room-backed journal. History is append-only: removals write corrections or
 * tombstones. A full wipe is the only table-clearing delete and stays on this
 * device.
 */
internal class JournalRepository(
    private val dao: JournalDao,
) {
    /** Every stored observation, including tombstones. */
    fun all(): List<JournalObservation> =
        dao.observations().map { entity -> toObservation(entity) }

    /** Observations that are still the current journal. */
    fun current(): List<JournalObservation> = JournalCommitter.current(all())

    /**
     * Appends a partial report. Returns null when the form recorded nothing.
     * [eventId] makes a watch retry append only once.
     */
    fun save(
        committedAt: Instant,
        edits: List<SeriesEdit>,
        confirmations: List<SeriesKey>,
        readings: List<SensorReadingDraft> = emptyList(),
        correctsObservationId: String? = null,
        eventId: String? = null,
    ): JournalObservation? {
        if (eventId != null && dao.seenEvent(eventId) != null) {
            return null
        }
        val commit = JournalCommitter.commit(
            committedAt = committedAt,
            prior = all(),
            edits = edits,
            confirmations = confirmations,
            newId = UUID.randomUUID().toString(),
            sensorReadings = readings,
            snapshotId = if (readings.isEmpty()) null else UUID.randomUUID().toString(),
        )
        val observation = commit.observation ?: return null
        val stored = observation.copy(correctsObservationId = correctsObservationId)
        insert(stored, commit.snapshot)
        if (eventId != null) {
            dao.insertSeenEvent(SeenEventEntity(eventId))
        }
        return stored
    }

    /** Appends a tombstone for [targetId]. */
    fun deleteReport(targetId: String, deletedAt: Instant): JournalObservation {
        val marker = JournalCommitter.delete(
            targetId = targetId,
            deletedAt = deletedAt,
            newId = UUID.randomUUID().toString(),
        )
        insert(marker, snapshot = null)
        return marker
    }

    /**
     * Removes one measurement from a current observation.
     *
     * Other measurements in the same save stay visible. Returns null when the
     * observation or series is no longer current.
     */
    fun deleteMeasurement(
        observationId: String,
        bodyPart: String,
        side: String?,
        symptom: String,
        deletedAt: Instant,
    ): JournalObservation? {
        val target = current().firstOrNull { observation -> observation.id == observationId }
            ?: return null
        val marker = JournalCommitter.removeMeasurement(
            target = target,
            bodyPart = bodyPart,
            side = side,
            symptom = symptom,
            deletedAt = deletedAt,
            newId = UUID.randomUUID().toString(),
        ) ?: return null
        insert(marker, snapshot = null)
        return marker
    }

    /** Removes the local tables. Does not contact a server. */
    fun wipe() {
        dao.deleteReadings()
        dao.deleteSnapshots()
        dao.deleteMeasurements()
        dao.deleteObservations()
        dao.deleteSeenEvents()
    }

    /** Current journal as JSON. Tombstones and corrected rows are omitted. */
    fun exportCurrentJson(): String {
        val rows = buildJsonArray {
            current().forEach { observation ->
                add(observationJson(observation))
            }
        }
        return rows.toString()
    }

    private fun insert(observation: JournalObservation, snapshot: JournalSnapshot?) {
        dao.insertObservation(
            ObservationEntity(
                id = observation.id,
                observedAtEpochMs = observation.observedAt.toEpochMilli(),
                correctsObservationId = observation.correctsObservationId,
                deletedAtEpochMs = observation.deletedAt?.toEpochMilli(),
            ),
        )
        dao.insertMeasurements(
            observation.measurements.map { measurement ->
                MeasurementEntity(
                    observationId = observation.id,
                    bodyPart = measurement.bodyPart,
                    side = measurement.side,
                    symptom = measurement.symptom,
                    rating = measurement.rating,
                    reportKind = measurement.reportKind,
                    previousObservationId = measurement.previousObservationId,
                    previousRating = measurement.previousRating,
                    previousObservedAtEpochMs = measurement.previousObservedAt?.toEpochMilli(),
                )
            },
        )
        if (snapshot != null) {
            dao.insertSnapshot(
                SnapshotEntity(
                    id = snapshot.id,
                    observationId = snapshot.observationId,
                    capturedAtEpochMs = snapshot.capturedAt.toEpochMilli(),
                ),
            )
            dao.insertReadings(
                snapshot.readings.map { reading ->
                    ReadingEntity(
                        snapshotId = snapshot.id,
                        quantity = reading.quantity,
                        state = reading.state,
                        value = reading.value,
                        unit = reading.unit,
                        source = reading.source,
                        capturedAtEpochMs = reading.capturedAt.toEpochMilli(),
                    )
                },
            )
        }
    }

    private fun toObservation(entity: ObservationEntity): JournalObservation {
        val measurements = dao.measurements(entity.id).map { row ->
            JournalMeasurement(
                bodyPart = row.bodyPart,
                side = row.side,
                symptom = row.symptom,
                rating = row.rating,
                reportKind = row.reportKind,
                previousObservationId = row.previousObservationId,
                previousRating = row.previousRating,
                previousObservedAt = row.previousObservedAtEpochMs?.let(Instant::ofEpochMilli),
            )
        }
        return JournalObservation(
            id = entity.id,
            observedAt = Instant.ofEpochMilli(entity.observedAtEpochMs),
            correctsObservationId = entity.correctsObservationId,
            deletedAt = entity.deletedAtEpochMs?.let(Instant::ofEpochMilli),
            measurements = measurements,
        )
    }

    private fun observationJson(observation: JournalObservation) = buildJsonObject {
        put("id", observation.id)
        put("observedAt", observation.observedAt.toString())
        put(
            "measurements",
            JsonArray(
                observation.measurements.map { measurement ->
                    buildJsonObject {
                        put("bodyPart", measurement.bodyPart)
                        put("side", measurement.side?.let(::JsonPrimitive) ?: JsonNull)
                        put("symptom", measurement.symptom)
                        put("rating", measurement.rating)
                        put("reportKind", measurement.reportKind)
                    }
                },
            ),
        )
    }
}
