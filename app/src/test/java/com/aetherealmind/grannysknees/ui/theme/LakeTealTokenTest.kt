package com.aetherealmind.grannysknees.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * DE-01: locked lake-teal / soft-navy tokens must not drift back to cream paper
 * or purple brand accents.
 */
class LakeTealTokenTest {
    @Test
    fun `light primary is lake teal`() {
        assertEquals(Color(0xFF0E7C86), BrandColours.LakeTeal)
    }

    @Test
    fun `light ground is cool paper not cream scrapbook`() {
        assertEquals(Color(0xFFF4F8F9), BrandColours.CoolPaper)
        assertNotEquals(Color(0xFFF6E7D4), BrandColours.CoolPaper)
    }

    @Test
    fun `dark primary is mint teal on soft navy`() {
        assertEquals(Color(0xFF7FE0E8), BrandColours.MintTeal)
        assertEquals(Color(0xFF152033), BrandColours.SoftNavy)
        assertNotEquals(Color(0xFF161310), BrandColours.SoftNavy)
    }

    @Test
    fun `rough uses coral secondary not brand primary`() {
        assertEquals(Color(0xFFC24E2D), BrandColours.Rough)
        assertNotEquals(BrandColours.LakeTeal, BrandColours.Rough)
        assertNotEquals(Color(0xFF6B4C9A), BrandColours.LakeTeal)
        assertNotEquals(Color(0xFF6B4C9A), BrandColours.MintTeal)
    }
}
