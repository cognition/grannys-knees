package com.example.grannysknees.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.File
import java.io.IOException
import java.time.Instant

/**
 * App-private JSON file cache for [ForecastCacheEnvelope].
 *
 * Read/write failures yield `null` / no-op respectively — never invented payloads.
 */
internal class ForecastCacheStore(
    private val file: File,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun read(): ForecastCacheEnvelope? {
        if (!file.exists()) return null
        return try {
            val text = file.readText(Charsets.UTF_8)
            parseEnvelope(text)
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: kotlinx.serialization.SerializationException) {
            null
        }
    }

    fun write(envelope: ForecastCacheEnvelope) {
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        val payload = buildJsonObject {
            put("fetchedAtEpochMs", envelope.fetchedAt.toEpochMilli())
            put("locationLabel", envelope.locationLabel)
            put("latitude", envelope.latitude)
            put("longitude", envelope.longitude)
            put("timezone", envelope.timezone)
            put("rawBody", envelope.rawBody)
        }.toString()
        file.writeText(payload, Charsets.UTF_8)
    }

    fun clear() {
        if (file.exists()) {
            file.delete()
        }
    }

    private fun parseEnvelope(text: String): ForecastCacheEnvelope? {
        val root = json.parseToJsonElement(text).jsonObject
        val fetchedAtMs = root.long("fetchedAtEpochMs") ?: return null
        val label = root.string("locationLabel") ?: return null
        val latitude = root.double("latitude") ?: return null
        val longitude = root.double("longitude") ?: return null
        val timezone = root.string("timezone") ?: return null
        val rawBody = root.string("rawBody") ?: return null
        if (rawBody.isBlank()) return null
        return ForecastCacheEnvelope(
            fetchedAt = Instant.ofEpochMilli(fetchedAtMs),
            locationLabel = label,
            latitude = latitude,
            longitude = longitude,
            timezone = timezone,
            rawBody = rawBody,
        )
    }

    private fun JsonObject.long(key: String): Long? =
        this[key]?.jsonPrimitive?.longOrNull

    private fun JsonObject.double(key: String): Double? =
        this[key]?.jsonPrimitive?.doubleOrNull

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull
}
