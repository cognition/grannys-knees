package com.example.grannysknees.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

/**
 * Persistence for the once-per-local-day forecast fetch stamp.
 */
class DailyFetchStampStoreTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `missing file reads as null`() {
        val store = DailyFetchStampStore(temporaryFolder.newFile("missing-stamp.txt").also { it.delete() })

        assertNull(store.readLocalDate())
    }

    @Test
    fun `round trips a local date`() {
        val store = DailyFetchStampStore(temporaryFolder.newFile("stamp.txt"))
        val day = LocalDate.of(2026, 11, 8)

        store.writeLocalDate(day)

        assertEquals(day, store.readLocalDate())
    }

    @Test
    fun `clear removes the stamp`() {
        val store = DailyFetchStampStore(temporaryFolder.newFile("clear-stamp.txt"))
        store.writeLocalDate(LocalDate.of(2026, 11, 8))

        store.clear()

        assertNull(store.readLocalDate())
    }
}
