package com.example.grannysknees.sayings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Parses the packaged granny-says bands and maps scores onto them. */
class GrannySaysCatalogTest {

    private val catalog: GrannySaysCatalog by lazy {
        GrannySaysCatalog.parse(canonicalJson())
    }

    @Test
    fun `score zero uses band one`() {
        val sayings = catalog.sayingsForScore(0)
        assertTrue(sayings.isNotEmpty())
        assertEquals(catalog.sayingsForScore(1), sayings)
    }

    @Test
    fun `each score from one to ten has sayings`() {
        (1..10).forEach { score ->
            val sayings = catalog.sayingsForScore(score)
            assertTrue("score $score", sayings.isNotEmpty())
            assertTrue(sayings.any { it.tone == GrannyTone.WRY })
        }
    }

    @Test
    fun `each score has ten additional sayings`() {
        (1..10).forEach { score ->
            assertTrue("score $score", catalog.sayingsForScore(score).size >= 16)
        }
    }

    @Test
    fun `loads bonus and wildcard ranges into every covered score`() {
        // Bonus + wildcards use minIndex/maxIndex and must land in those bands.
        assertTrue(
            "score 5 should include ranged sayings",
            catalog.sayingsForScore(5).size >= 100,
        )
        assertTrue(
            catalog.sayingsForScore(1).any {
                it.text == "The knees approve. Carry on."
            },
        )
        assertTrue(
            catalog.sayingsForScore(4).any {
                it.text == "Take the day as it comes. The knees certainly are."
            },
        )
    }

    @Test
    fun `free-speech section lines require the free-speech toggle`() {
        val gated = catalog.sayingsForScore(7).filter { it.requiresFreeSpeech }
        assertTrue(gated.size >= 15)
    }

    @Test
    fun `out of range scores clamp into the supported window`() {
        assertEquals(catalog.sayingsForScore(1), catalog.sayingsForScore(-3))
        assertEquals(catalog.sayingsForScore(10), catalog.sayingsForScore(99))
    }

    @Test
    fun `winter wording is tagged as seasonal`() {
        assertTrue(
            catalog.sayingsForScore(6).any {
                it.season == GrannySeason.WINTER && it.text.contains("shovel")
            },
        )
    }

    @Test
    fun `personal comparison sayings are gated to matching scores`() {
        val agree = catalog.sayingsForScore(4).single {
            it.text.startsWith("Standard says four; personalized says four.")
        }
        assertTrue(agree.requiresPersonalAgreement)
        val differ = catalog.sayingsForScore(4).single {
            it.text.startsWith("Standard says four; personalized says five.")
        }
        assertTrue(differ.requiresPersonalDifference)
        assertEquals(5, differ.expectedPersonalScore)
    }

    private fun canonicalJson(): String {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "assets/granny-says.json")
            if (candidate.isFile) return candidate.readText()
            dir = dir.parentFile
        }
        error("Could not locate assets/granny-says.json")
    }
}
