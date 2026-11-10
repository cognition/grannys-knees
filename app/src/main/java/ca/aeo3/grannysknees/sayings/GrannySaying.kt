package ca.aeo3.grannysknees.sayings

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

/** Broad season used to keep distinctly wintery lines out of the rest of the year. */
internal enum class GrannySeason {
    ALL_YEAR,
    WINTER,
    SPRING,
    SUMMER,
    AUTUMN,
    ;

    companion object {
        fun fromLabel(label: String?): GrannySeason? = when (label?.trim()?.lowercase()) {
            null, "" -> null
            "all_year", "all-year", "all year" -> ALL_YEAR
            "winter" -> WINTER
            "spring" -> SPRING
            "summer" -> SUMMER
            "autumn", "fall" -> AUTUMN
            else -> null
        }

        /** Canadian winter gets a little runway on either side of the calendar season. */
        fun current(month: Int): GrannySeason = when (month) {
            11, 12, 1, 2, 3 -> WINTER
            4, 5 -> SPRING
            6, 7, 8 -> SUMMER
            else -> AUTUMN
        }
    }
}

/** One Granny line for a weather-index band. */
internal data class GrannySaying(
    val tone: GrannyTone,
    val text: String,
    /** Only eligible when personalized and standard scores are both present and match. */
    val requiresPersonalAgreement: Boolean = false,
    /** Only eligible when the personalized and standard scores differ. */
    val requiresPersonalDifference: Boolean = false,
    /**
     * When set with [requiresPersonalDifference], the saying is only eligible if the
     * personalized score equals this claimed value.
     */
    val expectedPersonalScore: Int? = null,
    /** Only eligible when the user explicitly enables free-speech mode. */
    val requiresFreeSpeech: Boolean = false,
    val season: GrannySeason = GrannySeason.ALL_YEAR,
)
