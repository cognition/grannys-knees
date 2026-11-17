package com.aetherealmind.grannysknees.journal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ache rate ladder: bolt count / crossed pair, brightness within each tier.
 * Rating 3 opens the two-bolt tier (1–2 stay on a single bolt).
 */
class AcheBoltLayoutTest {
    @Test
    fun `zero rating draws nothing`() {
        assertEquals(AcheBoltLayout.None, acheBoltLayout(0))
    }

    @Test
    fun `one through two is a single bolt that brightens`() {
        val low = acheBoltLayout(1) as AcheBoltLayout.Parallel
        val high = acheBoltLayout(2) as AcheBoltLayout.Parallel
        assertEquals(1, low.count)
        assertEquals(1, high.count)
        assertEquals(0f, low.brightness, 0.0001f)
        assertEquals(1f, high.brightness, 0.0001f)
    }

    @Test
    fun `three opens the second bolt tier through five`() {
        val atThree = acheBoltLayout(3) as AcheBoltLayout.Parallel
        val atFive = acheBoltLayout(5) as AcheBoltLayout.Parallel
        assertEquals(2, atThree.count)
        assertEquals(2, atFive.count)
        assertEquals(0f, atThree.brightness, 0.0001f)
        assertEquals(1f, atFive.brightness, 0.0001f)
    }

    @Test
    fun `six through eight is three bolts`() {
        val mid = acheBoltLayout(7) as AcheBoltLayout.Parallel
        assertEquals(3, mid.count)
        assertTrue(mid.brightness in 0f..1f)
    }

    @Test
    fun `nine and ten are crossed bolts`() {
        assertTrue(acheBoltLayout(9) is AcheBoltLayout.Crossed)
        assertTrue(acheBoltLayout(10) is AcheBoltLayout.Crossed)
        val dim = acheBoltLayout(9) as AcheBoltLayout.Crossed
        val bright = acheBoltLayout(10) as AcheBoltLayout.Crossed
        assertEquals(0f, dim.brightness, 0.0001f)
        assertEquals(1f, bright.brightness, 0.0001f)
    }
}
