package com.example.grannysknees.sayings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull
import java.io.InputStream

/**
 * Parsed Granny Says bands keyed by weather-index score 1–10.
 *
 * Score 0 uses band 1. Values outside 0–10 clamp into that window.
 */
internal class GrannySaysCatalog(
    private val bands: Map<Int, List<GrannySaying>>,
) {
    /** Sayings for [score], or an empty list when that band is missing. */
    fun sayingsForScore(score: Int): List<GrannySaying> {
        val index = score.coerceIn(MIN_SCORE, MAX_SCORE).let { value ->
            if (value == 0) 1 else value
        }
        return bands[index].orEmpty()
    }

    companion object {
        private const val MIN_SCORE: Int = 0
        private const val MAX_SCORE: Int = 10

        private val json: Json = Json { ignoreUnknownKeys = true }

        /** Parses the `grannySays` document from [text]. */
        fun parse(text: String): GrannySaysCatalog {
            val root = json.parseToJsonElement(text).jsonObject
            val rows = (root["grannySays"]?.jsonArray.orEmpty() +
                root["additionalSayings"]?.jsonArray.orEmpty() +
                root["freeSpeechSayings"]?.jsonArray.orEmpty())
            val bands = linkedMapOf<Int, List<GrannySaying>>()
            rows.forEach { element ->
                val row = element.jsonObject
                val index = row["index"]?.jsonPrimitive?.intOrNull ?: return@forEach
                if (index !in 1..MAX_SCORE) return@forEach
                val sayings = row["sayings"]?.jsonArray.orEmpty().mapNotNull { sayingElement ->
                    val saying = sayingElement.jsonObject
                    val toneLabel = saying["tone"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val tone = GrannyTone.fromLabel(toneLabel) ?: return@mapNotNull null
                    val body = saying["text"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                    if (body.isEmpty()) return@mapNotNull null
                    GrannySaying(
                        tone = tone,
                        text = body,
                        requiresPersonalDifference = saying["requiresPersonalDifference"]
                            ?.jsonPrimitive?.booleanOrNull == true,
                        requiresFreeSpeech = saying["requiresFreeSpeech"]
                            ?.jsonPrimitive?.booleanOrNull == true,
                    )
                }
                if (sayings.isNotEmpty()) {
                    bands[index] = bands[index].orEmpty() + sayings
                }
            }
            return GrannySaysCatalog(bands)
        }

        /** Loads and parses an asset stream. */
        fun fromStream(stream: InputStream): GrannySaysCatalog =
            parse(stream.bufferedReader().use { reader -> reader.readText() })
    }
}
