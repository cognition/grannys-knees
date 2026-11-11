package com.aetherealmind.grannysknees.ui.outlook

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Day ribbon colouring: rectangles keep a day-relative vertical gradient.
 */
class DayRibbonTest {
    @Test
    fun `day high is darkest and day low is lightest`() {
        val pale = Color(0xFFB5DEE3)
        val deep = Color(0xFF0A5A62)
        val dayLow = 3f
        val dayHigh = 7f
        val atLow = scoreRibbonColour(
            score = dayLow,
            lowColour = pale,
            highColour = deep,
            dayLow = dayLow,
            dayHigh = dayHigh,
        )
        val atHigh = scoreRibbonColour(
            score = dayHigh,
            lowColour = pale,
            highColour = deep,
            dayLow = dayLow,
            dayHigh = dayHigh,
        )
        val mid = scoreRibbonColour(
            score = 5f,
            lowColour = pale,
            highColour = deep,
            dayLow = dayLow,
            dayHigh = dayHigh,
        )

        assertEquals(pale, atLow)
        assertEquals(deep, atHigh)
        assertTrue(atHigh.luminance() < mid.luminance())
        assertTrue(mid.luminance() < atLow.luminance())
    }

    @Test
    fun `flat day uses the mid-tone between light and dark`() {
        val pale = Color(0xFFB5DEE3)
        val deep = Color(0xFF0A5A62)
        val flat = scoreRibbonColour(
            score = 6f,
            lowColour = pale,
            highColour = deep,
            dayLow = 6f,
            dayHigh = 6f,
        )

        assertTrue(flat.luminance() < pale.luminance())
        assertTrue(flat.luminance() > deep.luminance())
    }

    @Test
    fun `flat day when every hour shares the same score`() {
        assertTrue(isFlatDay(listOf(5f, 5f, 5f)))
        assertFalse(isFlatDay(listOf(5f, 5.2f, 4f)))
    }

    @Test
    fun `smooth resampling keeps tip and end scores near the source`() {
        val scores = listOf(3f, 7f, 4f)
        val smoothed = smoothScores(
            scores = scores,
            sampleCount = 9,
            blurPasses = 1,
        )

        assertEquals(9, smoothed.size)
        assertEquals(scores.first(), smoothed.first(), 1.5f)
        assertEquals(scores.last(), smoothed.last(), 1.5f)
    }

    @Test
    fun `blur softens a sharp spike without changing length`() {
        val blurred = blurScores(listOf(4f, 10f, 4f), passes = 2)

        assertEquals(3, blurred.size)
        assertTrue(blurred[1] < 10f)
        assertTrue(blurred[1] > blurred[0])
    }
}
