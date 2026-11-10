package ca.aeo3.grannysknees.journal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Taps on the How you feel body land on the matching part and side. */
class BodyMapTest {
    @Test
    fun `the left knee is the knee on the left of the figure`() {
        val spot = BodyMap.spotAt(43f, 114.5f)

        assertEquals("knee", spot?.part)
        assertEquals("left", spot?.side)
        assertEquals("Left knee", bodySpotName(spot?.part.orEmpty(), spot?.side))
    }

    @Test
    fun `the head has no side and empty space selects nothing`() {
        val head = BodyMap.spotAt(50f, 12f)

        assertEquals("head", head?.part)
        assertNull(head?.side)
        assertNull(BodyMap.spotAt(0f, 0f))
    }
}
