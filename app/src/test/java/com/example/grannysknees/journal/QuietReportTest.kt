package com.example.grannysknees.journal

import org.junit.Assert.assertEquals
import org.junit.Test

/** Quiet report lines used by the history screen. */
class QuietReportTest {
    @Test
    fun `a changed report reads as one sentence and one detail`() {
        assertEquals("Left knee ache", quietReportTitle("left", "knee", "ache"))
        assertEquals(
            "4 · changed · Tue 16:10",
            quietReportDetail(4, JournalCommitter.CHANGED, "Tue 16:10"),
        )
    }

    @Test
    fun `a confirmation is labelled unchanged and a part with no side stays short`() {
        assertEquals("Back stiffness", quietReportTitle(null, "back", "stiffness"))
        assertEquals(
            "4 · unchanged · Mon 08:20",
            quietReportDetail(4, JournalCommitter.CONFIRMED_UNCHANGED, "Mon 08:20"),
        )
    }
}
