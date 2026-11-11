package com.aetherealmind.grannysknees.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Persists the About-screen appearance choice on the phone. */
class ThemePreferenceStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `missing file means follow the phone`() {
        val store = ThemePreferenceStore(File(temporaryFolder.root, "missing-theme.txt"))
        assertEquals(ThemePreference.SYSTEM, store.read())
    }

    @Test
    fun `round trips light and dark`() {
        val file = temporaryFolder.newFile("theme-preference.txt")
        val store = ThemePreferenceStore(file)
        store.write(ThemePreference.LIGHT)
        assertEquals(ThemePreference.LIGHT, store.read())
        store.write(ThemePreference.DARK)
        assertEquals(ThemePreference.DARK, store.read())
        store.write(ThemePreference.SYSTEM)
        assertEquals(ThemePreference.SYSTEM, store.read())
    }

    @Test
    fun `unreadable junk falls back to system`() {
        val file = temporaryFolder.newFile("theme-preference.txt")
        file.writeText("purple", Charsets.UTF_8)
        assertEquals(ThemePreference.SYSTEM, ThemePreferenceStore(file).read())
    }

    @Test
    fun `resolves dark only when forced or phone is dark`() {
        assertEquals(false, ThemePreference.LIGHT.resolveDark(systemDark = true))
        assertEquals(true, ThemePreference.DARK.resolveDark(systemDark = false))
        assertEquals(true, ThemePreference.SYSTEM.resolveDark(systemDark = true))
        assertEquals(false, ThemePreference.SYSTEM.resolveDark(systemDark = false))
    }
}
