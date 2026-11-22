package com.aetherealmind.grannysknees

import android.app.Application
import androidx.room.Room
import com.aetherealmind.grannysknees.journal.JournalDatabase
import com.aetherealmind.grannysknees.journal.JournalRepository

/**
 * Application entry for the phone app.
 *
 * Owns the single [JournalDatabase] instance so Journal, History, and Settings
 * do not each open a separate Room connection to `journal.db`.
 */
class GrannysKneesApplication : Application() {

    /** Process-wide journal database; created on first use. */
    internal val journalDatabase: JournalDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            JournalDatabase::class.java,
            JOURNAL_DB_NAME,
        ).build()
    }

    /** Repository bound to [journalDatabase]. */
    internal fun journalRepository(): JournalRepository = JournalRepository(journalDatabase)

    companion object {
        const val JOURNAL_DB_NAME: String = "journal.db"
    }
}
