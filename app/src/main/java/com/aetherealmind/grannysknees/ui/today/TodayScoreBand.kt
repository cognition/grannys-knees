package com.aetherealmind.grannysknees.ui.today

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Presentational 0–10 band for the experimental knee-weather index.
 *
 * The labels mirror the display bands in the weather/index contract (§8.1).
 * This type is a UI concern only: it performs no scoring and makes no medical
 * claim. The underlying score is produced elsewhere (T04); here it is purely
 * fake/injected data used to lay the screen out.
 */
internal enum class TodayScoreBand(val scores: IntRange) {
    Calm(0..2),
    Mild(3..4),
    Noticeable(5..6),
    Rough(7..8),
    Stormy(9..10),
    ;

    internal companion object {
        /** Lowest value of the experimental 0–10 index. */
        const val MIN_SCORE: Int = 0

        /** Highest value of the experimental 0–10 index. */
        const val MAX_SCORE: Int = 10

        private val ONE_DP = 1

        /**
         * Formats the index for on-screen use: one decimal, no "/10".
         */
        fun formatScore(score: BigDecimal): String =
            score.setScale(ONE_DP, RoundingMode.HALF_UP).toPlainString()

        /**
         * Map a whole-number score to its display band.
         *
         * Out-of-range input is clamped into the supported 0–10 window so the
         * UI always has a band to show, even when handed unexpected fake data.
         */
        fun forScore(score: Int): TodayScoreBand {
            val clamped = score.coerceIn(MIN_SCORE, MAX_SCORE)
            return entries.first { clamped in it.scores }
        }

        /**
         * Map a one-decimal index to its band via half-up whole number.
         */
        fun forScore(score: BigDecimal): TodayScoreBand =
            forScore(score.setScale(0, RoundingMode.HALF_UP).intValueExact())
    }
}

