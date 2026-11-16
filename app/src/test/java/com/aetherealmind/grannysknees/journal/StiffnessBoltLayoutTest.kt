package com.aetherealmind.grannysknees.journal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stiffness rate ladder: bolt count / crossed pair, brightness within each tier.
 * Rating 3 opens the two-bolt tier (1–2 stay on a single bolt).
 */
class StiffnessBoltLayoutTest {
    @Test
    fun `zero rating draws nothing`() {
        assertEquals(StiffnessBoltLayout.None, stiffnessBoltLayout(0))
    }

    @Test
    fun `one through two is a single bolt that brightens`() {
        val low = stiffnessBoltLayout(1) as StiffnessBoltLayout.Parallel
        val high = stiffnessBoltLayout(2) as StiffnessBoltLayout.Parallel
        assertEquals(1, low.count)
        assertEquals(1, high.count)
        assertEquals(0f, low.brightness, 0.0001f)
        assertEquals(1f, high.brightness, 0.0001f)
    }

    @Test
    fun `three opens the second bolt tier through five`() {
        val atThree = stiffnessBoltLayout(3) as StiffnessBoltLayout.Parallel
        val atFive = stiffnessBoltLayout(5) as StiffnessBoltLayout.Parallel
        assertEquals(2, atThree.count)
        assertEquals(2, atFive.count)
        assertEquals(0f, atThree.brightness, 0.0001f)
        assertEquals(1f, atFive.brightness, 0.0001f)
    }

    @Test
    fun `six through eight is three bolts`() {
        val mid = stiffnessBoltLayout(7) as StiffnessBoltLayout.Parallel
        assertEquals(3, mid.count)
        assertTrue(mid.brightness in 0f..1f)
    }

    @Test
    fun `nine and ten are crossed bolts`() {
        assertTrue(stiffnessBoltLayout(9) is StiffnessBoltLayout.Crossed)
        assertTrue(stiffnessBoltLayout(10) is StiffnessBoltLayout.Crossed)
        val dim = stiffnessBoltLayout(9) as StiffnessBoltLayout.Crossed
        val bright = stiffnessBoltLayout(10) as StiffnessBoltLayout.Crossed
        assertEquals(0f, dim.brightness, 0.0001f)
        assertEquals(1f, bright.brightness, 0.0001f)
    }
}
