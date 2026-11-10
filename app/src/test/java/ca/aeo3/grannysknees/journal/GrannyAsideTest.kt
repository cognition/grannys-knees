package ca.aeo3.grannysknees.journal

import ca.aeo3.grannysknees.R
import org.junit.Assert.assertEquals
import org.junit.Test

/** Granny's line follows the place. */
class GrannyAsideTest {
    @Test
    fun `the knee is the line she expected`() {
        assertEquals(R.string.journal_granny_knee, grannyAside("knee", "left"))
    }

    @Test
    fun `either hand gets the same line`() {
        assertEquals(R.string.journal_granny_hand, grannyAside("hand", "right"))
        assertEquals(R.string.journal_granny_hand, grannyAside("hand", "left"))
        assertEquals(R.string.journal_granny_other, grannyAside("other", null))
    }
}
