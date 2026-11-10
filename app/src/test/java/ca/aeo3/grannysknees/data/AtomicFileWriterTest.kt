package ca.aeo3.grannysknees.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AtomicFileWriterTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `creates nested file with payload`() {
        val target = File(temporaryFolder.root, "prefs/cache.json")
        val payload = """{"ok":true}"""

        AtomicFileWriter.writeText(target, payload)

        assertEquals(payload, target.readText(Charsets.UTF_8))
    }

    @Test
    fun `replaces prior contents`() {
        val target = temporaryFolder.newFile("stamp.txt")
        AtomicFileWriter.writeText(target, "2026-11-09")

        AtomicFileWriter.writeText(target, "2026-11-10")

        assertEquals("2026-11-10", target.readText(Charsets.UTF_8))
    }

    @Test
    fun `does not leave backup sibling after successful write`() {
        val target = temporaryFolder.newFile("theme.txt")
        AtomicFileWriter.writeText(target, "dark")

        val backup = File(target.parent, target.name + ".bak")
        assertFalse("Expected no .bak after finishWrite", backup.exists())
    }
}
