package com.aetherealmind.grannysknees.weather

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.time.ZoneId
import java.time.zone.ZoneRulesException

/** Thrown when an Open-Meteo payload cannot be parsed into [HourlyForecast]. */
class OpenMeteoParseException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * Parses a raw Open-Meteo `/v1/forecast` JSON body (requested with
 * `timeformat=unixtime`) into [HourlyForecast], performing DST-correct IANA
 * conversion of every epoch.
 *
 * Contract rules enforced (`docs/contracts/weather-index.md`):
 * - §3.2: the received `hourly_units` are asserted to match the contract's
 *   metric units (°C, %, hPa) and `unixtime`; a mismatch is a parse error.
 * - §4.1: each `hourly.time[]` entry is treated as a UTC epoch second and
 *   converted through the response's IANA `timezone` with [java.time]; the
 *   single request-time offset is never applied per-row.
 * - §7: a JSON `null` in a value array is preserved as `null`, never `0`.
 *
 * This parser derives no index score, pressure trend, daily extrema, or cache
 * freshness — those are out of T03 scope.
 */
object OpenMeteoParser {

    private const val EXPECTED_TIME_UNIT = "unixtime"
    private const val EXPECTED_TEMPERATURE_UNIT = "°C"
    private const val EXPECTED_HUMIDITY_UNIT = "%"
    private const val EXPECTED_PRESSURE_UNIT = "hPa"

    private const val KEY_TEMPERATURE = "temperature_2m"
    private const val KEY_HUMIDITY = "relative_humidity_2m"
    private const val KEY_PRESSURE = "surface_pressure"
    private const val KEY_WEATHER_CODE = "weather_code"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * @throws OpenMeteoParseException if the body is malformed, is missing a
     *   required field or variable, has mismatched units, or carries
     *   inconsistent array lengths.
     */
    fun parse(body: String): HourlyForecast {
        val root = try {
            json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            throw OpenMeteoParseException("Response body is not a JSON object", e)
        }

        val zone = parseZone(root)
        val units = parseUnits(root)
        assertUnits(units)

        val hourly = root.obj("hourly")
        val epochs = hourly.arr("time").map { element ->
            element.longValue() ?: throw OpenMeteoParseException(
                "hourly.time contains a non-integer or null entry",
            )
        }

        val temperatures = hourly.optionalDoubleColumn(KEY_TEMPERATURE, epochs.size)
        val humidities = hourly.optionalIntColumn(KEY_HUMIDITY, epochs.size)
        val pressures = hourly.optionalDoubleColumn(KEY_PRESSURE, epochs.size)
        val weatherCodes = hourly.optionalIntColumnIfPresent(KEY_WEATHER_CODE, epochs.size)

        val hours = epochs.mapIndexed { i, epoch ->
            val instant = Instant.ofEpochSecond(epoch)
            val local = instant.atZone(zone)
            ForecastHour(
                epoch = epoch,
                instant = instant,
                local = local,
                utcOffsetSeconds = local.offset.totalSeconds,
                localDate = local.toLocalDate(),
                temperature2m = temperatures[i],
                relativeHumidity2m = humidities[i],
                surfacePressure = pressures[i],
                weatherCode = weatherCodes?.get(i),
            )
        }

        return HourlyForecast(
            location = ForecastLocation(
                latitude = root.requireDouble("latitude"),
                longitude = root.requireDouble("longitude"),
                elevation = root["elevation"]?.doubleOrNullSafe(),
            ),
            timezone = ForecastTimezone(
                zone = zone,
                abbreviationAtRequest = root["timezone_abbreviation"]?.contentOrNull(),
                utcOffsetSecondsAtRequest = root.requireInt("utc_offset_seconds"),
            ),
            generationTimeMs = root["generationtime_ms"]?.doubleOrNullSafe(),
            units = units,
            hours = hours,
        )
    }

    private fun parseZone(root: JsonObject): ZoneId {
        val name = root["timezone"]?.contentOrNull()
            ?: throw OpenMeteoParseException("Response is missing a `timezone`")
        return try {
            ZoneId.of(name)
        } catch (e: ZoneRulesException) {
            throw OpenMeteoParseException("Unknown IANA timezone `$name`", e)
        } catch (e: Exception) {
            throw OpenMeteoParseException("Invalid timezone `$name`", e)
        }
    }

    private fun parseUnits(root: JsonObject): HourlyUnits {
        val u = root.obj("hourly_units")
        return HourlyUnits(
            time = u.requireString("time"),
            temperature2m = u.requireString(KEY_TEMPERATURE),
            relativeHumidity2m = u.requireString(KEY_HUMIDITY),
            surfacePressure = u.requireString(KEY_PRESSURE),
            weatherCode = u[KEY_WEATHER_CODE]?.contentOrNull(),
        )
    }

    private fun assertUnits(units: HourlyUnits) {
        checkUnit("time", units.time, EXPECTED_TIME_UNIT)
        checkUnit(KEY_TEMPERATURE, units.temperature2m, EXPECTED_TEMPERATURE_UNIT)
        checkUnit(KEY_HUMIDITY, units.relativeHumidity2m, EXPECTED_HUMIDITY_UNIT)
        checkUnit(KEY_PRESSURE, units.surfacePressure, EXPECTED_PRESSURE_UNIT)
    }

    private fun checkUnit(name: String, actual: String, expected: String) {
        if (actual != expected) {
            throw OpenMeteoParseException(
                "Unexpected unit for `$name`: expected `$expected`, got `$actual`",
            )
        }
    }

    // --- JsonObject / JsonElement helpers -----------------------------------

    private fun JsonObject.obj(key: String): JsonObject =
        (this[key] ?: throw OpenMeteoParseException("Response is missing `$key`")).let {
            it as? JsonObject
                ?: throw OpenMeteoParseException("`$key` is not an object")
        }

    private fun JsonObject.arr(key: String): JsonArray =
        (this[key] ?: throw OpenMeteoParseException("`$key` is missing")).let {
            it as? JsonArray
                ?: throw OpenMeteoParseException("`$key` is not an array")
        }

    private fun JsonObject.requireString(key: String): String =
        this[key]?.contentOrNull()
            ?: throw OpenMeteoParseException("`$key` is missing or not a string")

    private fun JsonObject.requireDouble(key: String): Double =
        this[key]?.doubleOrNullSafe()
            ?: throw OpenMeteoParseException("`$key` is missing or not a number")

    private fun JsonObject.requireInt(key: String): Int =
        this[key]?.intOrNullSafe()
            ?: throw OpenMeteoParseException("`$key` is missing or not an integer")

    /**
     * Reads a required hourly `Double` column, preserving nulls. The column
     * must be present (the contract requires all three variables) and exactly
     * as long as the time axis.
     */
    private fun JsonObject.optionalDoubleColumn(key: String, expectedSize: Int): List<Double?> {
        val values = requireColumn(key, expectedSize).map { it.doubleOrNullPreservingNull(key) }
        return values
    }

    private fun JsonObject.optionalIntColumn(key: String, expectedSize: Int): List<Int?> =
        requireColumn(key, expectedSize).map { it.intOrNullPreservingNull(key) }

    private fun JsonObject.optionalIntColumnIfPresent(key: String, expectedSize: Int): List<Int?>? =
        if (this[key] == null) null else optionalIntColumn(key, expectedSize)

    private fun JsonObject.requireColumn(key: String, expectedSize: Int): JsonArray {
        val array = arr(key)
        if (array.size != expectedSize) {
            throw OpenMeteoParseException(
                "`hourly.$key` has ${array.size} entries but `hourly.time` has $expectedSize",
            )
        }
        return array
    }

    /** `null` for JSON null; parsed Double otherwise; error for anything else. */
    private fun JsonElement.doubleOrNullPreservingNull(key: String): Double? {
        if (this is JsonNull) return null
        return (this as? JsonPrimitive)?.doubleOrNull
            ?: throw OpenMeteoParseException("`$key` contains a non-numeric entry `$this`")
    }

    /** `null` for JSON null; parsed Int otherwise; error for anything else. */
    private fun JsonElement.intOrNullPreservingNull(key: String): Int? {
        if (this is JsonNull) return null
        return (this as? JsonPrimitive)?.intOrNull
            ?: throw OpenMeteoParseException("`$key` contains a non-integer entry `$this`")
    }

    private fun JsonElement.longValue(): Long? =
        (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content?.toLongOrNull()

    private fun JsonElement.contentOrNull(): String? =
        (this as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonElement.doubleOrNullSafe(): Double? =
        (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.doubleOrNull

    private fun JsonElement.intOrNullSafe(): Int? =
        (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.intOrNull
}
