package com.example.grannysknees.sayings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Weighted wry preference and refresh behaviour for Granny Says. */
class GrannySaysPickerTest {

    private val band: List<GrannySaying> = listOf(
        GrannySaying(GrannyTone.WARM, "warm-a"),
        GrannySaying(GrannyTone.WARM, "warm-b"),
        GrannySaying(GrannyTone.WRY, "wry-a"),
        GrannySaying(GrannyTone.WRY, "wry-b"),
        GrannySaying(GrannyTone.SPICY, "spicy-a"),
        GrannySaying(GrannyTone.SPICY, "spicy-b"),
    )

    @Test
    fun `first show uses forty percent wry`() {
        assertEquals(
            ToneWeights(wry = 40, warm = 30, spicy = 30),
            ToneWeights.forRefreshIndex(0),
        )
    }

    @Test
    fun `each refresh raises wry by ten until eighty`() {
        assertEquals(ToneWeights(50, 25, 25), ToneWeights.forRefreshIndex(1))
        assertEquals(ToneWeights(60, 20, 20), ToneWeights.forRefreshIndex(2))
        assertEquals(ToneWeights(70, 15, 15), ToneWeights.forRefreshIndex(3))
        assertEquals(ToneWeights(80, 10, 10), ToneWeights.forRefreshIndex(4))
        assertEquals(ToneWeights(80, 10, 10), ToneWeights.forRefreshIndex(9))
    }

    @Test
    fun `forced wry draw picks a wry line`() {
        val picker = GrannySaysPicker(band, random = Random(0))
        val saying = picker.pick(ToneWeights(wry = 100, warm = 0, spicy = 0), excludeText = null)
        assertEquals(GrannyTone.WRY, saying!!.tone)
    }

    @Test
    fun `refresh avoids repeating the same text when another line exists`() {
        val picker = GrannySaysPicker(band, random = Random(1))
        val first = picker.showForScore(7)!!
        val second = picker.refresh()!!
        assertNotEquals(first.text, second.text)
    }

    @Test
    fun `score change resets the wry climb`() {
        val picker = GrannySaysPicker(band, random = Random(2))
        picker.showForScore(4)
        picker.refresh()
        picker.refresh()
        assertEquals(2, picker.refreshIndex)
        picker.showForScore(8)
        assertEquals(0, picker.refreshIndex)
        assertEquals(ToneWeights(40, 30, 30), picker.currentWeights())
    }

    @Test
    fun `many wry-heavy draws stay mostly wry`() {
        val picker = GrannySaysPicker(band, random = Random(42))
        picker.showForScore(5)
        repeat(4) { picker.refresh() }
        var wryHits = 0
        repeat(200) {
            if (picker.refresh()!!.tone == GrannyTone.WRY) {
                wryHits += 1
            }
        }
        assertTrue("expected mostly wry, got $wryHits", wryHits > 140)
    }

    @Test
    fun `personal difference sayings are hidden unless scores differ`() {
        val conditional = GrannySaying(
            tone = GrannyTone.WRY,
            text = "different",
            requiresPersonalDifference = true,
        )
        val picker = GrannySaysPicker(listOf(GrannySaying(GrannyTone.WARM, "usual"), conditional), random = Random(3))

        picker.showForScore(5, personalScore = 5)
        assertEquals("usual", picker.pick(ToneWeights(wry = 0, warm = 100, spicy = 0), null)!!.text)
        picker.showForScore(5, personalScore = 7)
        assertEquals("different", picker.pick(ToneWeights(wry = 100, warm = 0, spicy = 0), null)!!.text)
    }

    @Test
    fun `personal agreement sayings are hidden when scores differ`() {
        val agree = GrannySaying(
            tone = GrannyTone.WRY,
            text = "Standard says four; personalized says four. Even the fussy numbers agree.",
            requiresPersonalAgreement = true,
        )
        val usual = GrannySaying(GrannyTone.WARM, "usual")
        val picker = GrannySaysPicker(listOf(usual, agree), random = Random(7))

        picker.showForScore(4, personalScore = 2)
        assertEquals("usual", picker.pick(ToneWeights(wry = 100, warm = 0, spicy = 0), null)!!.text)
        picker.showForScore(4, personalScore = 4)
        assertEquals(agree.text, picker.pick(ToneWeights(wry = 100, warm = 0, spicy = 0), null)!!.text)
    }

    @Test
    fun `personal difference sayings require the claimed personal score`() {
        val differ = GrannySaying(
            tone = GrannyTone.WRY,
            text = "Standard says four; personalized says five.",
            requiresPersonalDifference = true,
            expectedPersonalScore = 5,
        )
        val usual = GrannySaying(GrannyTone.WARM, "usual")
        val picker = GrannySaysPicker(listOf(usual, differ), random = Random(8))

        picker.showForScore(4, personalScore = 2)
        assertEquals("usual", picker.pick(ToneWeights(wry = 100, warm = 0, spicy = 0), null)!!.text)
        picker.showForScore(4, personalScore = 5)
        assertEquals(differ.text, picker.pick(ToneWeights(wry = 100, warm = 0, spicy = 0), null)!!.text)
    }

    @Test
    fun `free speech sayings are hidden by default`() {
        val clean = GrannySaying(GrannyTone.WARM, "clean")
        val salty = GrannySaying(GrannyTone.WRY, "salty", requiresFreeSpeech = true)
        val picker = GrannySaysPicker(listOf(clean, salty), random = Random(4))

        picker.showForScore(5)
        assertEquals("clean", picker.pick(ToneWeights(wry = 100, warm = 0, spicy = 0), null)!!.text)
        picker.showForScore(5, allowFreeSpeech = true)
        assertEquals("salty", picker.pick(ToneWeights(wry = 100, warm = 0, spicy = 0), null)!!.text)
    }

    @Test
    fun `winter sayings are hidden outside winter`() {
        val allYear = GrannySaying(GrannyTone.WARM, "all year")
        val winter = GrannySaying(GrannyTone.WRY, "winter", season = GrannySeason.WINTER)
        val picker = GrannySaysPicker(listOf(allYear, winter), random = Random(5))

        picker.showForScore(5, season = GrannySeason.SUMMER)
        assertEquals("all year", picker.pick(ToneWeights(wry = 100, warm = 0, spicy = 0), null)!!.text)
        picker.showForScore(5, season = GrannySeason.WINTER)
        assertEquals("winter", picker.pick(ToneWeights(wry = 100, warm = 0, spicy = 0), null)!!.text)
    }

    @Test
    fun `snow forecast enables winter sayings outside winter`() {
        val allYear = GrannySaying(GrannyTone.WARM, "all year")
        val winter = GrannySaying(GrannyTone.WRY, "winter", season = GrannySeason.WINTER)
        val picker = GrannySaysPicker(listOf(allYear, winter), random = Random(6))

        picker.showForScore(5, season = GrannySeason.SUMMER, snowInForecast = true)
        assertEquals("winter", picker.pick(ToneWeights(wry = 100, warm = 0, spicy = 0), null)!!.text)
    }
}
