package com.aetherealmind.grannysknees.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.File
import java.io.IOException
import java.time.Instant

/**
 * App-private JSON list of Feel calibration time-points.
 *
 * A missing or unreadable file means no samples yet. Samples without
 * [FeelCalibration.DRIVER_SCHEMA] (legacy 12 h deltas) are dropped on read.
 */
internal class FeelCalibrationStore(
    private val file: File,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val lock = Any()
    private var schemaResetOccurred: Boolean = false

    /** Every stored slope-schema sample, oldest first. */
    fun read(): List<FeelCalibrationSample> = synchronized(lock) {
        readUnlocked()
    }

    /**
     * True once after a [read] dropped incompatible samples and rewrote the
     * file; clears the latch so callers can show a one-time notice.
     */
    fun consumeSchemaResetOccurred(): Boolean = synchronized(lock) {
        val occurred = schemaResetOccurred
        schemaResetOccurred = false
        occurred
    }

    /** Replaces the sample list. */
    fun write(samples: List<FeelCalibrationSample>) {
        synchronized(lock) {
            writeUnlocked(samples)
        }
    }

    /** Clears all calibration samples. */
    fun clear() {
        synchronized(lock) {
            if (file.exists()) {
                file.delete()
            }
        }
    }

    /**
     * Upserts a Feel save into the store when weather context is usable.
     *
     * @return updated samples (unchanged when [capture] has no window mean)
     */
    fun recordSave(
        observedAt: Instant,
        observationId: String,
        ratings: List<Int>,
        capture: FeelWeatherCapture,
    ): List<FeelCalibrationSample> = synchronized(lock) {
        val windowMean = capture.weatherWindowMean ?: return@synchronized readUnlocked()
        if (ratings.isEmpty()) return@synchronized readUnlocked()
        val incoming = FeelCalibrationSample(
            centreAt = observedAt,
            ratings = ratings,
            weatherWindowMean = windowMean,
            deltaPressure = capture.deltaPressure,
            deltaHumidity = capture.deltaHumidity,
            deltaTemperature = capture.deltaTemperature,
            observationIds = listOf(observationId),
            driverSchema = FeelCalibration.DRIVER_SCHEMA,
        )
        val next = FeelCalibration.upsertSample(readUnlocked(), incoming)
        writeUnlocked(next)
        next
    }

    private fun readUnlocked(): List<FeelCalibrationSample> {
        if (!file.exists()) return emptyList()
        return try {
            val parsed = parseAll(file.readText(Charsets.UTF_8))
            val compatible = parsed.filter { sample ->
                sample.driverSchema == FeelCalibration.DRIVER_SCHEMA
            }
            if (compatible.size != parsed.size) {
                writeUnlocked(compatible)
                schemaResetOccurred = true
            }
            compatible
        } catch (_: IOException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        } catch (_: kotlinx.serialization.SerializationException) {
            emptyList()
        }
    }

    private fun writeUnlocked(samples: List<FeelCalibrationSample>) {
        val payload = buildJsonObject {
            put(
                "samples",
                buildJsonArray {
                    samples.forEach { sample ->
                        add(sampleJson(sample))
                    }
                },
            )
        }.toString()
        AtomicFileWriter.writeText(file, payload)
    }

    private fun sampleJson(sample: FeelCalibrationSample): JsonObject = buildJsonObject {
        put("centreAtEpochMs", sample.centreAt.toEpochMilli())
        put(
            "ratings",
            buildJsonArray {
                sample.ratings.forEach { rating -> add(JsonPrimitive(rating)) }
            },
        )
        put("weatherWindowMean", sample.weatherWindowMean)
        sample.deltaPressure?.let { put("deltaPressure", it) }
        sample.deltaHumidity?.let { put("deltaHumidity", it) }
        sample.deltaTemperature?.let { put("deltaTemperature", it) }
        put("driverSchema", sample.driverSchema)
        put(
            "observationIds",
            buildJsonArray {
                sample.observationIds.forEach { id -> add(JsonPrimitive(id)) }
            },
        )
    }

    private fun parseAll(text: String): List<FeelCalibrationSample> {
        val root = json.parseToJsonElement(text).jsonObject
        val array = root["samples"]?.jsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element.jsonObject
            val centreMs = obj["centreAtEpochMs"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val ratings = obj["ratings"]?.jsonArray?.mapNotNull { rating ->
                rating.jsonPrimitive.intOrNull
            }.orEmpty()
            if (ratings.isEmpty()) return@mapNotNull null
            val weather = obj["weatherWindowMean"]?.jsonPrimitive?.doubleOrNull
                ?: return@mapNotNull null
            val ids = obj["observationIds"]?.jsonArray?.mapNotNull { id ->
                id.jsonPrimitive.contentOrNull
            }.orEmpty()
            FeelCalibrationSample(
                centreAt = Instant.ofEpochMilli(centreMs),
                ratings = ratings,
                weatherWindowMean = weather,
                deltaPressure = obj["deltaPressure"]?.jsonPrimitive?.doubleOrNull,
                deltaHumidity = obj["deltaHumidity"]?.jsonPrimitive?.doubleOrNull,
                deltaTemperature = obj["deltaTemperature"]?.jsonPrimitive?.doubleOrNull,
                observationIds = ids,
                driverSchema = obj["driverSchema"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            )
        }.sortedBy { it.centreAt }
    }
}
