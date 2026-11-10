package ca.aeo3.grannysknees.data

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
 * A missing or unreadable file means no samples yet.
 */
internal class FeelCalibrationStore(
    private val file: File,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    /** Every stored sample, oldest first. */
    fun read(): List<FeelCalibrationSample> {
        if (!file.exists()) return emptyList()
        return try {
            parse(file.readText(Charsets.UTF_8))
        } catch (_: IOException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        } catch (_: kotlinx.serialization.SerializationException) {
            emptyList()
        }
    }

    /** Replaces the sample list. */
    fun write(samples: List<FeelCalibrationSample>) {
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

    /** Clears all calibration samples. */
    fun clear() {
        if (file.exists()) {
            file.delete()
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
    ): List<FeelCalibrationSample> {
        val windowMean = capture.weatherWindowMean ?: return read()
        if (ratings.isEmpty()) return read()
        val incoming = FeelCalibrationSample(
            centreAt = observedAt,
            ratings = ratings,
            weatherWindowMean = windowMean,
            deltaPressure = capture.deltaPressure,
            deltaHumidity = capture.deltaHumidity,
            deltaTemperature = capture.deltaTemperature,
            observationIds = listOf(observationId),
        )
        val next = FeelCalibration.upsertSample(read(), incoming)
        write(next)
        return next
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
        put(
            "observationIds",
            buildJsonArray {
                sample.observationIds.forEach { id -> add(JsonPrimitive(id)) }
            },
        )
    }

    private fun parse(text: String): List<FeelCalibrationSample> {
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
            )
        }.sortedBy { it.centreAt }
    }
}
