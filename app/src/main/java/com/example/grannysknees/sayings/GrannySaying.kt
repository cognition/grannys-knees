package com.example.grannysknees.sayings

/** Tone for a Granny line in the weather-index list. */
internal enum class GrannyTone {
    WARM,
    WRY,
    SPICY,
    ;

    companion object {
        /** Maps the JSON tone label onto a known tone, or null when unknown. */
        fun fromLabel(label: String): GrannyTone? = when (label.trim().lowercase()) {
            "warm" -> WARM
            "wry" -> WRY
            "spicy" -> SPICY
            else -> null
        }
    }
}

/** One Granny line for a weather-index band. */
internal data class GrannySaying(
    val tone: GrannyTone,
    val text: String,
)
