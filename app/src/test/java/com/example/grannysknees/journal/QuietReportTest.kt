package com.example.grannysknees.journal

import org.junit.Assert.assertEquals
import org.junit.Test

/** Quiet report lines used by the history screen. */
class QuietReportTest {
    @Test
    fun `a changed report reads as one headline and one detail`() {
        assertEquals("Left knee ache", quietReportTitle("left", "knee", "ache"))
        assertEquals(
            "Left knee ache, 4 - Tue 16:10",
            quietHistoryHeadline("left", "knee", "ache", 4, "Tue 16:10"),
        )
        assertEquals("changed", quietReportDetail(JournalCommitter.CHANGED))
    }

    @Test
    fun `a confirmation is labelled unchanged and a part with no side stays short`() {
        assertEquals("Back stiffness", quietReportTitle(null, "back", "stiffness"))
        assertEquals(
            "Back stiffness, 4 - Mon 08:20",
            quietHistoryHeadline(null, "back", "stiffness", 4, "Mon 08:20"),
        )
        assertEquals(
            "unchanged",
            quietReportDetail(JournalCommitter.CONFIRMED_UNCHANGED),
        )
    }

    @Test
    fun `history headline puts the rating beside the label before the date`() {
        assertEquals(
            "Left wrist stiffness, 8 - Wed 10 Nov · 09:15",
            quietHistoryHeadline(
                "left",
                "wrist",
                "stiffness",
                8,
                "Wed 10 Nov · 09:15",
            ),
        )
    }
}
