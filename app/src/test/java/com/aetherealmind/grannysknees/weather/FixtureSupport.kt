package com.aetherealmind.grannysknees.weather

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File

/**
 * Reads the canonical contract fixtures straight from `docs/contracts/fixtures/`
 * rather than copying them into test resources, so these tests exercise the same
 * artefacts the contract pins and cannot silently drift from them.
 */
object FixtureSupport {

    private val fixturesDir: File by lazy { locateFixturesDir() }

    fun inputBody(case: String): String =
        File(fixturesDir, "$case.input.json").readText()

    fun expectedHours(case: String): List<ExpectedHour> {
        val root = Json.parseToJsonElement(File(fixturesDir, "$case.expected.json").readText())
            .jsonObject
        return root["hours"]!!.jsonArray.map { it.jsonObject.toExpectedHour() }
    }

    private fun JsonObject.toExpectedHour(): ExpectedHour {
        val inputs = this["inputs"]!!.jsonObject
        return ExpectedHour(
            epoch = (this["epoch"] as JsonPrimitive).content.toLong(),
            instantUtc = (this["instantUtc"] as JsonPrimitive).content,
            local = (this["local"] as JsonPrimitive).content,
            utcOffsetSeconds = (this["utcOffsetSeconds"] as JsonPrimitive).content.toInt(),
            localDate = (this["localDate"] as JsonPrimitive).content,
            surfacePressure = inputs["surface_pressure"].asDoubleOrNull(),
            relativeHumidity2m = inputs["relative_humidity_2m"].asIntOrNull(),
            temperature2m = inputs["temperature_2m"].asDoubleOrNull(),
        )
    }

    private fun kotlinx.serialization.json.JsonElement?.asDoubleOrNull(): Double? =
        if (this == null || this is JsonNull) null else (this as JsonPrimitive).doubleOrNull

    private fun kotlinx.serialization.json.JsonElement?.asIntOrNull(): Int? =
        if (this == null || this is JsonNull) null else (this as JsonPrimitive).intOrNull

    /** Walks up from the test working directory until `docs/contracts/fixtures` is found. */
    private fun locateFixturesDir(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "docs/contracts/fixtures")
            if (candidate.isDirectory) return candidate
            dir = dir.parentFile
        }
        error("Could not locate docs/contracts/fixtures from ${System.getProperty("user.dir")}")
    }
}

/**
 * The subset of a `*.expected.json` hour that is T03's responsibility: the
 * instant/offset/local fields derived by IANA conversion and the raw provider
 * inputs. The scorer-owned fields (subScores, indexScore, …) are intentionally
 * not read here.
 */
data class ExpectedHour(
    val epoch: Long,
    val instantUtc: String,
    val local: String,
    val utcOffsetSeconds: Int,
    val localDate: String,
    val surfacePressure: Double?,
    val relativeHumidity2m: Int?,
    val temperature2m: Double?,
)
