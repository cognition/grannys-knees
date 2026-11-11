package com.aetherealmind.grannysknees.ui.theme

import com.aetherealmind.grannysknees.data.AtomicFileWriter
import java.io.File
import java.io.IOException

/** Appearance choice from Settings. Missing preference follows the phone. */
internal enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    /** Whether the lake-teal dark palette should be active. */
    fun resolveDark(systemDark: Boolean): Boolean =
        when (this) {
            SYSTEM -> systemDark
            LIGHT -> false
            DARK -> true
        }
}

/**
 * App-private text file for the appearance choice.
 *
 * A missing or unreadable file means follow the phone.
 */
internal class ThemePreferenceStore(
    private val file: File,
) {
    /** Saved preference, or [ThemePreference.SYSTEM] when unset. */
    fun read(): ThemePreference {
        if (!file.exists()) {
            return ThemePreference.SYSTEM
        }
        return try {
            when (file.readText(Charsets.UTF_8).trim()) {
                "light" -> ThemePreference.LIGHT
                "dark" -> ThemePreference.DARK
                "system" -> ThemePreference.SYSTEM
                else -> ThemePreference.SYSTEM
            }
        } catch (_: IOException) {
            ThemePreference.SYSTEM
        }
    }

    /** Replaces the saved appearance choice. */
    fun write(preference: ThemePreference) {
        val token = when (preference) {
            ThemePreference.SYSTEM -> "system"
            ThemePreference.LIGHT -> "light"
            ThemePreference.DARK -> "dark"
        }
        AtomicFileWriter.writeText(file, token)
    }
}
