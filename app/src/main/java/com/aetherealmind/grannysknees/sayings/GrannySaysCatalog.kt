package com.aetherealmind.grannysknees.sayings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream

/**
 * Parsed Granny Says bands keyed by weather-index score 1–10.
 *
 * Loads every packaged list: `grannySays`, `additionalSayings`,
 * `freeSpeechSayings`, plus ranged `bonusSayings` and `wildcards`.
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

        /** Parses the full `granny-says` document from [text]. */
        fun parse(text: String): GrannySaysCatalog {
            val root = json.parseToJsonElement(text).jsonObject
            val bands = linkedMapOf<Int, MutableList<GrannySaying>>()

            mergeIndexedBands(
                bands = bands,
                rows = root["grannySays"]?.jsonArray.orEmpty(),
                forceFreeSpeech = false,
            )
            mergeIndexedBands(
                bands = bands,
                rows = root["additionalSayings"]?.jsonArray.orEmpty(),
                forceFreeSpeech = false,
            )
            mergeIndexedBands(
                bands = bands,
                rows = root["freeSpeechSayings"]?.jsonArray.orEmpty(),
                forceFreeSpeech = true,
            )
            mergeRangedSayings(
                bands = bands,
                rows = root["bonusSayings"]?.jsonArray.orEmpty(),
                forceFreeSpeech = false,
            )
            mergeRangedSayings(
                bands = bands,
                rows = root["wildcards"]?.jsonArray.orEmpty(),
                forceFreeSpeech = false,
            )

            return GrannySaysCatalog(
                bands.mapValues { (_, sayings) -> dedupeByText(sayings) },
            )
        }

        /** Loads and parses an asset stream. */
        fun fromStream(stream: InputStream): GrannySaysCatalog =
            parse(stream.bufferedReader().use { reader -> reader.readText() })

        private fun mergeIndexedBands(
            bands: MutableMap<Int, MutableList<GrannySaying>>,
            rows: List<JsonElement>,
            forceFreeSpeech: Boolean,
        ) {
            rows.forEach { element ->
                val row = element.jsonObject
                val index = row["index"]?.jsonPrimitive?.intOrNull ?: return@forEach
                if (index !in 1..MAX_SCORE) return@forEach
                val sayings = row["sayings"]?.jsonArray.orEmpty().mapNotNull { sayingElement ->
                    parseSaying(
                        saying = sayingElement.jsonObject,
                        forceFreeSpeech = forceFreeSpeech,
                    )
                }
                if (sayings.isNotEmpty()) {
                    bands.getOrPut(index) { mutableListOf() }.addAll(sayings)
                }
            }
        }

        private fun mergeRangedSayings(
            bands: MutableMap<Int, MutableList<GrannySaying>>,
            rows: List<JsonElement>,
            forceFreeSpeech: Boolean,
        ) {
            rows.forEach { element ->
                val row = element.jsonObject
                val saying = parseSaying(row, forceFreeSpeech = forceFreeSpeech) ?: return@forEach
                val minIndex = row["minIndex"]?.jsonPrimitive?.intOrNull ?: return@forEach
                val maxIndex = row["maxIndex"]?.jsonPrimitive?.intOrNull ?: return@forEach
                val start = minOf(minIndex, maxIndex).coerceIn(1, MAX_SCORE)
                val end = maxOf(minIndex, maxIndex).coerceIn(1, MAX_SCORE)
                for (index in start..end) {
                    bands.getOrPut(index) { mutableListOf() }.add(saying)
                }
            }
        }

        private fun parseSaying(
            saying: JsonObject,
            forceFreeSpeech: Boolean,
        ): GrannySaying? {
            val toneLabel = saying["tone"]?.jsonPrimitive?.contentOrNull ?: return null
            val tone = GrannyTone.fromLabel(toneLabel) ?: return null
            val body = saying["text"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (body.isEmpty()) return null
            val season = GrannySeason.fromLabel(saying["season"]?.jsonPrimitive?.contentOrNull)
                ?: if (WINTER_WORDS.any { word -> body.contains(word, ignoreCase = true) }) {
                    GrannySeason.WINTER
                } else {
                    GrannySeason.ALL_YEAR
                }
            val flaggedFreeSpeech = saying["requiresFreeSpeech"]
                ?.jsonPrimitive?.booleanOrNull == true
            return GrannySaying(
                tone = tone,
                text = body,
                requiresPersonalAgreement = saying["requiresPersonalAgreement"]
                    ?.jsonPrimitive?.booleanOrNull == true,
                requiresPersonalDifference = saying["requiresPersonalDifference"]
                    ?.jsonPrimitive?.booleanOrNull == true,
                expectedPersonalScore = saying["expectedPersonalScore"]
                    ?.jsonPrimitive?.intOrNull,
                requiresFreeSpeech = forceFreeSpeech || flaggedFreeSpeech,
                season = season,
            )
        }

        private fun dedupeByText(sayings: List<GrannySaying>): List<GrannySaying> {
            val seen = linkedSetOf<String>()
            return sayings.filter { saying ->
                seen.add(saying.text)
            }
        }

        private val WINTER_WORDS = listOf(
            "snow", "snowbank", "snow shovel", "shovel", "salt truck", "toque", "winter",
            "january", "december", "snow tires", "rink", "cold front", "freez",
            "ice storm", "blizzard",
        )
    }
}
