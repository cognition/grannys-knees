package ca.aeo3.grannysknees.sayings

/**
 * Percentage weights for picking a tone.
 *
 * First show starts at 40% wry. Each refresh raises wry by 10 points until
 * 80%, and the remainder is split evenly between warm and spicy.
 */
internal data class ToneWeights(
    val wry: Int,
    val warm: Int,
    val spicy: Int,
) {
    init {
        require(wry >= 0 && warm >= 0 && spicy >= 0) { "weights must be non-negative" }
        require(wry + warm + spicy == 100) { "weights must sum to 100" }
    }

    companion object {
        private const val FIRST_WRY: Int = 40
        private const val WRY_STEP: Int = 10
        private const val MAX_WRY: Int = 80

        /**
         * Weights for the [refreshIndex]-th pick of the current score.
         *
         * Zero is the first show. Larger values are successive refreshes.
         */
        fun forRefreshIndex(refreshIndex: Int): ToneWeights {
            val steps = refreshIndex.coerceAtLeast(0)
            val wry = (FIRST_WRY + steps * WRY_STEP).coerceAtMost(MAX_WRY)
            val rest = 100 - wry
            val warm = rest / 2
            val spicy = rest - warm
            return ToneWeights(wry = wry, warm = warm, spicy = spicy)
        }
    }
}
