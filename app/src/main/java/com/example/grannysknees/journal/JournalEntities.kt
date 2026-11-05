package com.example.grannysknees.journal

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "observations")
internal data class ObservationEntity(
    @PrimaryKey val id: String,
    val observedAtEpochMs: Long,
    val correctsObservationId: String?,
    val deletedAtEpochMs: Long?,
)

@Entity(tableName = "measurements")
internal data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val observationId: String,
    val bodyPart: String,
    val side: String?,
    val symptom: String,
    val rating: Int,
    val reportKind: String,
    val previousObservationId: String?,
    val previousRating: Int?,
    val previousObservedAtEpochMs: Long?,
)

@Entity(tableName = "snapshots")
internal data class SnapshotEntity(
    @PrimaryKey val id: String,
    val observationId: String,
    val capturedAtEpochMs: Long,
)

@Entity(tableName = "readings")
internal data class ReadingEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val snapshotId: String,
    val quantity: String,
    val state: String,
    val value: Double?,
    val unit: String?,
    val source: String,
    val capturedAtEpochMs: Long,
)

@Entity(tableName = "seen_events")
internal data class SeenEventEntity(
    @PrimaryKey val eventId: String,
)

@Dao
internal interface JournalDao {
    @Query("SELECT * FROM observations")
    fun observations(): List<ObservationEntity>

    @Query("SELECT * FROM measurements WHERE observationId = :observationId")
    fun measurements(observationId: String): List<MeasurementEntity>

    @Query("SELECT * FROM snapshots WHERE observationId = :observationId")
    fun snapshot(observationId: String): SnapshotEntity?

    @Query("SELECT * FROM readings WHERE snapshotId = :snapshotId")
    fun readings(snapshotId: String): List<ReadingEntity>

    @Query("SELECT eventId FROM seen_events WHERE eventId = :eventId")
    fun seenEvent(eventId: String): String?

    @Insert
    fun insertObservation(entity: ObservationEntity)

    @Insert
    fun insertMeasurements(entities: List<MeasurementEntity>)

    @Insert
    fun insertSnapshot(entity: SnapshotEntity)

    @Insert
    fun insertReadings(entities: List<ReadingEntity>)

    @Insert
    fun insertSeenEvent(entity: SeenEventEntity)

    @Query("DELETE FROM observations")
    fun deleteObservations()

    @Query("DELETE FROM measurements")
    fun deleteMeasurements()

    @Query("DELETE FROM snapshots")
    fun deleteSnapshots()

    @Query("DELETE FROM readings")
    fun deleteReadings()

    @Query("DELETE FROM seen_events")
    fun deleteSeenEvents()
}

@Database(
    entities = [
        ObservationEntity::class,
        MeasurementEntity::class,
        SnapshotEntity::class,
        ReadingEntity::class,
        SeenEventEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
internal abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
}
