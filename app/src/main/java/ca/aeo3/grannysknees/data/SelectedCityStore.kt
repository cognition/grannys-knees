package ca.aeo3.grannysknees.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.io.IOException

/**
 * App-private JSON file for the city the user picked.
 *
 * A missing or unreadable file means no city has been chosen. It never
 * invents Toronto.
 */
internal class SelectedCityStore(
    private val file: File,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    /** The saved city, or null when the user has not chosen one. */
    fun read(): SelectedCity? {
        if (!file.exists()) return null
        return try {
            parse(file.readText(Charsets.UTF_8))
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: kotlinx.serialization.SerializationException) {
            null
        }
    }

    /** Replaces the saved city. */
    fun write(city: SelectedCity) {
        val payload = buildJsonObject {
            put("label", city.label)
            put("latitude", city.latitude)
            put("longitude", city.longitude)
            put("timezone", city.timezone)
            put("admin1", city.admin1)
            put("countryCode", city.countryCode)
            put("placeSource", city.placeSource.name)
        }.toString()
        AtomicFileWriter.writeText(file, payload)
    }

    fun clear() {
        if (file.exists()) {
            file.delete()
        }
    }

    private fun parse(text: String): SelectedCity? {
        val root = json.parseToJsonElement(text).jsonObject
        val label = root.string("label") ?: return null
        val latitude = root.double("latitude") ?: return null
        val longitude = root.double("longitude") ?: return null
        val timezone = root.string("timezone") ?: return null
        val admin1 = root.string("admin1") ?: return null
        val countryCode = root.string("countryCode") ?: return null
        if (label.isBlank() || timezone.isBlank()) return null
        val placeSource = root.string("placeSource")
            ?.let { raw ->
                runCatching { PlaceSource.valueOf(raw) }.getOrNull()
            }
            ?: PlaceSource.Search
        return SelectedCity(
            label = label,
            latitude = latitude,
            longitude = longitude,
            timezone = timezone,
            admin1 = admin1,
            countryCode = countryCode,
            placeSource = placeSource,
        )
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.double(key: String): Double? =
        this[key]?.jsonPrimitive?.doubleOrNull
}
