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
    fun `out of range scores clamp into the supported window`() {
        assertEquals(catalog.sayingsForScore(1), catalog.sayingsForScore(-3))
        assertEquals(catalog.sayingsForScore(10), catalog.sayingsForScore(99))
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
