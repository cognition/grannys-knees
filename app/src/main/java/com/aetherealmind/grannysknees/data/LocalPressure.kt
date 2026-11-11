package com.aetherealmind.grannysknees.data

import com.aetherealmind.grannysknees.weather.HourlyForecast
import java.io.File
import java.time.Instant
import kotlin.math.abs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** A recent phone barometer sample, stored only on the device. */
internal data class LocalPressureReading(
    val pressureHpa: Double,
    val recordedAt: Instant,
)

/** Small, best-effort local store for the latest barometer sample. */
internal class LocalPressureStore(
    private val file: File,
    private val json: Json = Json,
) {
    fun read(): LocalPressureReading? {
        return try {
            if (!file.exists()) return null
            val root = json.parseToJsonElement(file.readText()).jsonObject
            val pressure = root["pressureHpa"]?.jsonPrimitive?.doubleOrNull ?: return null
            val epoch = root["recordedAtEpoch"]?.jsonPrimitive?.longOrNull ?: return null
            if (!pressure.isFinite() || pressure <= 0.0) null
            else LocalPressureReading(pressure, Instant.ofEpochSecond(epoch))
        } catch (_: Exception) {
            null
        }
    }

    fun write(reading: LocalPressureReading) {
        val payload = buildJsonObject {
            put("pressureHpa", reading.pressureHpa)
            put("recordedAtEpoch", reading.recordedAt.epochSecond)
        }.toString()
        AtomicFileWriter.writeText(file, payload)
    }
}

/**
 * Anchors only the nearest forecast hour to a recent local pressure reading.
 * The forecast remains the source for future hours; this avoids pretending a
 * single phone reading can predict the whole day.
 */
internal object LocalPressureBlend {
    const val LOCAL_WEIGHT: Double = 0.30
    const val MAX_READING_AGE_SECONDS: Long = 30 * 60
    const val MAX_FORECAST_DISTANCE_SECONDS: Long = 90 * 60

    fun pressureForCurrentHour(
        forecast: HourlyForecast,
        reading: LocalPressureReading?,
        now: Instant,
    ): Double? {
        if (reading == null) return null
        if (now.epochSecond - reading.recordedAt.epochSecond !in 0..MAX_READING_AGE_SECONDS) {
            return null
        }
        val hour = forecast.hours.minByOrNull { abs(it.epoch - now.epochSecond) } ?: return null
        if (abs(hour.epoch - now.epochSecond) > MAX_FORECAST_DISTANCE_SECONDS) return null
        val forecastPressure = hour.surfacePressure ?: return null
        return ((1.0 - LOCAL_WEIGHT) * forecastPressure) +
            (LOCAL_WEIGHT * reading.pressureHpa)
    }
}
