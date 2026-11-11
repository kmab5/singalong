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
    @ColumnInfo(defaultValue = "0") val favorite: Boolean = false,
    val fileName: String? = null,
)

@Entity(tableName = "occurrences", foreignKeys = [ForeignKey(entity = RecordingEntity::class, parentColumns = ["id"], childColumns = ["recordingId"], onDelete = ForeignKey.CASCADE)], indices = [Index("recordingId")])
data class OccurrenceEntity(@PrimaryKey val id: String, val recordingId: String)

@Entity(tableName = "lyrics", foreignKeys = [ForeignKey(entity = RecordingEntity::class, parentColumns = ["id"], childColumns = ["recordingId"], onDelete = ForeignKey.CASCADE)], indices = [Index("recordingId")])
data class LyricEntity(@PrimaryKey val id: String, val recordingId: String, val contentJson: String)

@Entity(tableName = "lyric_references", foreignKeys = [ForeignKey(entity = RecordingEntity::class, parentColumns = ["id"], childColumns = ["recordingId"], onDelete = ForeignKey.CASCADE)], indices = [Index("recordingId")])
data class LyricReferenceEntity(@PrimaryKey val id: String, val recordingId: String, val contentJson: String)

@Entity(tableName = "selections", foreignKeys = [
    ForeignKey(entity = RecordingEntity::class, parentColumns = ["id"], childColumns = ["recordingId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = LyricEntity::class, parentColumns = ["id"], childColumns = ["versionId"], onDelete = ForeignKey.CASCADE),
], indices = [Index("versionId")])
data class SelectionEntity(@PrimaryKey val recordingId: String, val versionId: String)

@Entity(tableName = "adjustments", foreignKeys = [ForeignKey(entity = LyricEntity::class, parentColumns = ["id"], childColumns = ["versionId"], onDelete = ForeignKey.CASCADE)])
data class AdjustmentEntity(@PrimaryKey val versionId: String, val delayMs: Long = 0, val allowUncertainSync: Boolean = false)

data class LibraryRow(@Embedded val recording: RecordingEntity, val hasLyrics: Boolean)

@Entity(tableName = "translations", foreignKeys = [ForeignKey(entity = LyricEntity::class, parentColumns = ["id"], childColumns = ["versionId"], onDelete = ForeignKey.CASCADE)])
data class TranslationEntity(@PrimaryKey val versionId: String, val contentJson: String)

@Dao
interface LibraryDao {
    @Query("SELECT r.*, EXISTS(SELECT 1 FROM lyrics l WHERE l.recordingId = r.id) AS hasLyrics FROM recordings r ORDER BY r.lastPlayedAtMs DESC, r.title COLLATE NOCASE")
    fun observeLibrary(): Flow<List<LibraryRow>>
    @Query("SELECT * FROM recordings WHERE id = :id") suspend fun recording(id: String): RecordingEntity?
    @Query("SELECT * FROM recordings WHERE normalizedTitle = :title") suspend fun candidates(title: String): List<RecordingEntity>
    @Query("SELECT * FROM translations WHERE versionId = :id") suspend fun translation(id: String): TranslationEntity?
    @Upsert suspend fun translation(value: TranslationEntity)
    @Query("SELECT * FROM recordings") suspend fun allRecordings(): List<RecordingEntity>
    @Query("SELECT * FROM recordings WHERE fileName IS NOT NULL") suspend fun folderRecordings(): List<RecordingEntity>
    @Query("UPDATE recordings SET favorite = :value WHERE id = :id") suspend fun favorite(id: String, value: Boolean)
    @Query("UPDATE recordings SET playCount = 0, lastPlayedAtMs = NULL") suspend fun clearHistory()
    @Query("DELETE FROM lyrics WHERE id = :id") suspend fun deleteVersion(id: String)
    @Upsert suspend fun upsert(recording: RecordingEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertOccurrence(value: OccurrenceEntity): Long
    @Query("UPDATE recordings SET playCount = playCount + 1, lastPlayedAtMs = CASE WHEN lastPlayedAtMs IS NULL OR lastPlayedAtMs < :at THEN :at ELSE lastPlayedAtMs END WHERE id = :id")
    suspend fun incrementPlay(id: String, at: Long)
    @Query("UPDATE recordings SET lookupStatus = :status WHERE id = :id") suspend fun lookupStatus(id: String, status: String)
    @Upsert suspend fun upsertLyrics(value: LyricEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertReference(value: LyricReferenceEntity): Long
    @Query("SELECT * FROM lyric_references WHERE recordingId = :id ORDER BY id") suspend fun references(id: String): List<LyricReferenceEntity>
    @Query("SELECT * FROM lyric_references WHERE id = :id") suspend fun reference(id: String): LyricReferenceEntity?
    @Query("SELECT * FROM lyrics WHERE recordingId = :id ORDER BY id") suspend fun versions(id: String): List<LyricEntity>
    @Query("SELECT l.* FROM lyrics l INNER JOIN selections s ON s.versionId = l.id WHERE s.recordingId = :id") suspend fun selected(id: String): LyricEntity?
    @Query("SELECT * FROM lyrics WHERE id = :id") suspend fun version(id: String): LyricEntity?
    @Upsert suspend fun select(value: SelectionEntity)
    @Query("SELECT * FROM adjustments WHERE versionId = :id") suspend fun adjustment(id: String): AdjustmentEntity?
    @Upsert suspend fun adjust(value: AdjustmentEntity)
}

@Database(entities = [RecordingEntity::class, OccurrenceEntity::class, LyricEntity::class, LyricReferenceEntity::class, SelectionEntity::class, AdjustmentEntity::class, TranslationEntity::class], version = 4, exportSchema = true)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    companion object {
        fun open(context: Context) = Room.databaseBuilder(context.applicationContext, LibraryDatabase::class.java, "singalong.db").addMigrations(object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE recordings ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0"); db.execSQL("CREATE TABLE IF NOT EXISTS translations (versionId TEXT NOT NULL PRIMARY KEY, contentJson TEXT NOT NULL, FOREIGN KEY(versionId) REFERENCES lyrics(id) ON DELETE CASCADE)") }
        }, FILE_NAME_MIGRATION, LYRIC_REFERENCES_MIGRATION).build()

        val FILE_NAME_MIGRATION = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recordings ADD COLUMN fileName TEXT")
            }
        }
        val LYRIC_REFERENCES_MIGRATION = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS lyric_references (id TEXT NOT NULL PRIMARY KEY, recordingId TEXT NOT NULL, contentJson TEXT NOT NULL, FOREIGN KEY(recordingId) REFERENCES recordings(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lyric_references_recordingId ON lyric_references (recordingId)")
            }
        }
    }
}
