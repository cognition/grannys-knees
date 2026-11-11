package com.aetherealmind.grannysknees.weather

import com.aetherealmind.grannysknees.data.PlaceSource
import com.aetherealmind.grannysknees.data.SelectedCity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Canadian city search against the Open-Meteo geocoding endpoint named in the
 * weather contract. Rows missing a name, province, coordinates, or timezone
 * are dropped.
 */
internal class OpenMeteoGeocodingClient(
    private val transport: HttpTransport = UrlHttpTransport(),
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    /**
     * Cities in Canada matching [name].
     *
     * A blank query is an empty list and does not call the network. A body
     * with no `results` array is an empty list, not an error.
     */
    @Throws(IOException::class)
    fun search(name: String): List<SelectedCity> {
        val query = name.trim()
        if (query.isEmpty()) return emptyList()
        return parse(transport.get(buildUrl(query)))
    }

    /** Search URL including `countryCode=CA`. */
    fun buildUrl(name: String): String {
        val query = listOf(
            "name" to name.trim(),
            "count" to "10",
            "language" to "en",
            "format" to "json",
            "countryCode" to "CA",
        ).joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return "$baseUrl?$query"
    }

    /** Parses a geocoding body. Incomplete rows are omitted. */
    fun parse(body: String): List<SelectedCity> {
        val root = json.parseToJsonElement(body).jsonObject
        val results = root["results"] ?: return emptyList()
        return results.jsonArray.mapNotNull { element ->
            val row = element.jsonObject
            val city = row.string("name") ?: return@mapNotNull null
            val admin1 = row.string("admin1") ?: return@mapNotNull null
            val latitude = row.double("latitude") ?: return@mapNotNull null
            val longitude = row.double("longitude") ?: return@mapNotNull null
            val timezone = row.string("timezone") ?: return@mapNotNull null
            val countryCode = row.string("country_code") ?: return@mapNotNull null
            SelectedCity(
                label = "$city, $admin1",
                latitude = latitude,
                longitude = longitude,
                timezone = timezone,
                admin1 = admin1,
                countryCode = countryCode,
                placeSource = PlaceSource.Search,
            )
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { value -> value.isNotEmpty() }

    private fun JsonObject.double(key: String): Double? =
        this[key]?.jsonPrimitive?.doubleOrNull

    companion object {
        const val DEFAULT_BASE_URL: String = "https://geocoding-api.open-meteo.com/v1/search"
    }
}
