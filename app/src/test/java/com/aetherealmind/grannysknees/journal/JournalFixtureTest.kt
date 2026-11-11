package com.aetherealmind.grannysknees.journal

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

/**
 * The five symptom-observation fixtures. Untouched series stay absent, and an
 * explicit zero is stored.
 */
class JournalFixtureTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `partial update stores only the left foot ache`() {
        val root = fixture("partial-update.json")
        val commit = commit(root, "obs-partial-1")
        val observation = requireNotNull(commit.observation)
        assertEquals(1, observation.measurements.size)
        val measurement = observation.measurements.single()
        assertEquals("foot", measurement.bodyPart)
        assertEquals("left", measurement.side)
        assertEquals("ache", measurement.symptom)
        assertEquals(6, measurement.rating)
        assertEquals(JournalCommitter.CHANGED, measurement.reportKind)
        assertAbsent(observation, "knee", "left", "ache")
        assertAbsent(observation, "foot", "left", "stiffness")
    }

    @Test
    fun `explicit zero is stored and the prior row is unchanged`() {
        val root = fixture("explicit-zero.json")
        val prior = prior(root)
        val commit = commit(root, "obs-zero-1")
        val measurement = requireNotNull(commit.observation).measurements.single()
        assertEquals(0, measurement.rating)
        assertEquals("obs-prior-knee", measurement.previousObservationId)
        assertEquals(5, measurement.previousRating)
        assertEquals(Instant.parse("2026-11-03T15:00:00.000Z"), prior.single().observedAt)
        assertEquals(5, prior.single().measurements.single().rating)
    }

    @Test
    fun `untouched save writes no observation`() {
        val root = fixture("untouched-symptoms.json")
        val commit = commit(root, "unused")
        assertNull(commit.observation)
        assertNull(commit.snapshot)
        assertEquals(Instant.parse("2026-11-03T15:00:00.000Z"), prior(root).single().observedAt)
    }

    @Test
    fun `confirmed unchanged keeps the original timestamp on the prior row`() {
        val root = fixture("confirmed-unchanged.json")
        val commit = commit(root, "obs-confirm-1")
        val measurement = requireNotNull(commit.observation).measurements.single()
        assertEquals(JournalCommitter.CONFIRMED_UNCHANGED, measurement.reportKind)
        assertEquals(4, measurement.rating)
        assertEquals("obs-prior-stiff", measurement.previousObservationId)
        assertEquals(Instant.parse("2026-11-02T18:30:00.000Z"), measurement.previousObservedAt)
        assertEquals(Instant.parse("2026-11-02T18:30:00.000Z"), prior(root).single().observedAt)
    }

    @Test
    fun `unavailable sensor value stays null and the index is not stored`() {
        val root = fixture("unavailable-sensors.json")
        val readings = sensorAttempts(root)
        val commit = JournalCommitter.commit(
            committedAt = Instant.parse(root.input().string("committedAt")!!),
            prior = emptyList(),
            edits = edits(root),
            confirmations = emptyList(),
            newId = "obs-sensor-1",
            sensorReadings = readings,
            snapshotId = "snap-sensor-1",
        )
        val pressure = requireNotNull(commit.snapshot).readings
            .single { reading -> reading.quantity == "surface_pressure" }
        assertEquals("unavailable", pressure.state)
        assertNull(pressure.value)
        assertTrue(commit.snapshot.readings.none { reading -> reading.quantity == "index" })
        assertTrue(
            commit.snapshot.readings.none { reading ->
                reading.quantity == "heart_rate" || reading.quantity == "steps"
            },
        )
    }

    private fun commit(root: JsonObject, newId: String): JournalCommit {
        val input = root.input()
        return JournalCommitter.commit(
            committedAt = Instant.parse(input.string("committedAt")!!),
            prior = prior(root),
            edits = edits(root),
            confirmations = confirmations(root),
            newId = newId,
        )
    }

    private fun prior(root: JsonObject): List<JournalObservation> {
        val rows = root.input()["priorObservations"]?.jsonArray ?: return emptyList()
        return rows.map { element ->
            val row = element.jsonObject
            JournalObservation(
                id = row.string("id")!!,
                observedAt = Instant.parse(row.string("observedAt")!!),
                correctsObservationId = null,
                deletedAt = null,
                measurements = row.measurements(),
            )
        }
    }

    private fun edits(root: JsonObject): List<SeriesEdit> =
        root.input()["edits"]?.jsonArray.orEmpty().map { element ->
            val row = element.jsonObject
            SeriesEdit(
                bodyPart = row.string("bodyPart")!!,
                side = row.string("side"),
                symptom = row.string("symptom")!!,
                rating = row.int("rating")!!,
            )
        }

    private fun confirmations(root: JsonObject): List<SeriesKey> =
        root.input()["confirmations"]?.jsonArray.orEmpty().map { element ->
            val row = element.jsonObject
            SeriesKey(
                bodyPart = row.string("bodyPart")!!,
                side = row.string("side"),
                symptom = row.string("symptom")!!,
            )
        }

    private fun sensorAttempts(root: JsonObject): List<SensorReadingDraft> =
        root.input()["sensorAttempts"]!!.jsonArray.map { element ->
            val row = element.jsonObject
            SensorReadingDraft(
                quantity = row.string("quantity")!!,
                state = row.string("state")!!,
                value = row["value"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
                unit = row.string("unit"),
                source = row.string("source")!!,
                capturedAt = Instant.parse(row.string("capturedAt")!!),
            )
        }

    private fun JsonObject.measurements(): List<JournalMeasurement> =
        this["measurements"]!!.jsonArray.map { element ->
            val row = element.jsonObject
            JournalMeasurement(
                bodyPart = row.string("bodyPart")!!,
                side = row.string("side"),
                symptom = row.string("symptom")!!,
                rating = row.int("rating")!!,
                reportKind = row.string("reportKind") ?: JournalCommitter.CHANGED,
                previousObservationId = null,
                previousRating = null,
                previousObservedAt = null,
            )
        }

    private fun assertAbsent(
        observation: JournalObservation,
        bodyPart: String,
        side: String,
        symptom: String,
    ) {
        assertTrue(
            observation.measurements.none { measurement ->
                measurement.bodyPart == bodyPart &&
                    measurement.side == side &&
                    measurement.symptom == symptom
            },
        )
    }

    private fun fixture(name: String): JsonObject {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "docs/contracts/fixtures/symptom-observations/$name")
            if (candidate.isFile) {
                return json.parseToJsonElement(candidate.readText()).jsonObject
            }
            dir = dir.parentFile
        }
        error("Could not locate $name")
    }

    private fun JsonObject.input(): JsonObject = this["input"]!!.jsonObject

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.int(key: String): Int? =
        this[key]?.jsonPrimitive?.intOrNull
}
