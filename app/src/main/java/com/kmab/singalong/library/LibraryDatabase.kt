package com.kmab.singalong.library

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recordings", indices = [Index("normalizedTitle")])
data class RecordingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val normalizedTitle: String,
    val artistsJson: String,
    val album: String?,
    val durationMs: Long?,
    val versionLabel: String?,
    val lastPlayedAtMs: Long? = null,
    val playCount: Long = 0,
    val lookupStatus: String = "not_requested",
)

@Entity(tableName = "occurrences", foreignKeys = [ForeignKey(entity = RecordingEntity::class, parentColumns = ["id"], childColumns = ["recordingId"], onDelete = ForeignKey.CASCADE)], indices = [Index("recordingId")])
data class OccurrenceEntity(@PrimaryKey val id: String, val recordingId: String)

@Entity(tableName = "lyrics", foreignKeys = [ForeignKey(entity = RecordingEntity::class, parentColumns = ["id"], childColumns = ["recordingId"], onDelete = ForeignKey.CASCADE)], indices = [Index("recordingId")])
data class LyricEntity(@PrimaryKey val id: String, val recordingId: String, val contentJson: String)

@Entity(tableName = "selections", foreignKeys = [
    ForeignKey(entity = RecordingEntity::class, parentColumns = ["id"], childColumns = ["recordingId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = LyricEntity::class, parentColumns = ["id"], childColumns = ["versionId"], onDelete = ForeignKey.CASCADE),
], indices = [Index("versionId")])
data class SelectionEntity(@PrimaryKey val recordingId: String, val versionId: String)

@Entity(tableName = "adjustments", foreignKeys = [ForeignKey(entity = LyricEntity::class, parentColumns = ["id"], childColumns = ["versionId"], onDelete = ForeignKey.CASCADE)])
data class AdjustmentEntity(@PrimaryKey val versionId: String, val delayMs: Long = 0, val allowUncertainSync: Boolean = false)

data class LibraryRow(@Embedded val recording: RecordingEntity, val hasLyrics: Boolean)

@Dao
interface LibraryDao {
    @Query("SELECT r.*, EXISTS(SELECT 1 FROM lyrics l WHERE l.recordingId = r.id) AS hasLyrics FROM recordings r ORDER BY r.lastPlayedAtMs DESC, r.title COLLATE NOCASE")
    fun observeLibrary(): Flow<List<LibraryRow>>
    @Query("SELECT * FROM recordings WHERE id = :id") suspend fun recording(id: String): RecordingEntity?
    @Query("SELECT * FROM recordings WHERE normalizedTitle = :title") suspend fun candidates(title: String): List<RecordingEntity>
    @Upsert suspend fun upsert(recording: RecordingEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertOccurrence(value: OccurrenceEntity): Long
    @Query("UPDATE recordings SET playCount = playCount + 1, lastPlayedAtMs = CASE WHEN lastPlayedAtMs IS NULL OR lastPlayedAtMs < :at THEN :at ELSE lastPlayedAtMs END WHERE id = :id")
    suspend fun incrementPlay(id: String, at: Long)
    @Query("UPDATE recordings SET lookupStatus = :status WHERE id = :id") suspend fun lookupStatus(id: String, status: String)
    @Upsert suspend fun upsertLyrics(value: LyricEntity)
    @Query("SELECT * FROM lyrics WHERE recordingId = :id ORDER BY id") suspend fun versions(id: String): List<LyricEntity>
    @Query("SELECT l.* FROM lyrics l INNER JOIN selections s ON s.versionId = l.id WHERE s.recordingId = :id") suspend fun selected(id: String): LyricEntity?
    @Query("SELECT * FROM lyrics WHERE id = :id") suspend fun version(id: String): LyricEntity?
    @Upsert suspend fun select(value: SelectionEntity)
    @Query("SELECT * FROM adjustments WHERE versionId = :id") suspend fun adjustment(id: String): AdjustmentEntity?
    @Upsert suspend fun adjust(value: AdjustmentEntity)
}

@Database(entities = [RecordingEntity::class, OccurrenceEntity::class, LyricEntity::class, SelectionEntity::class, AdjustmentEntity::class], version = 1, exportSchema = true)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    companion object {
        fun open(context: Context) = Room.databaseBuilder(context.applicationContext, LibraryDatabase::class.java, "singalong.db").build()
    }
}
