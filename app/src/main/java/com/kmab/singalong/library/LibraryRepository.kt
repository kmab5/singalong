package com.kmab.singalong.library

import androidx.room.withTransaction
import com.kmab.singalong.model.*
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import com.kmab.singalong.backup.*
import com.kmab.singalong.translation.*

interface LyricsStore {
    suspend fun selectedVersion(recordingId: String): LyricVersion?
    suspend fun saveCandidates(recording: Recording, versions: List<LyricVersion>)
    suspend fun setLookupStatus(recordingId: String, status: String)
    suspend fun storedStatus(recordingId: String): String? = null
}

interface ReaderStore {
    suspend fun selectedVersion(recordingId: String): LyricVersion?
    suspend fun versions(recordingId: String): List<LyricVersion>
    suspend fun selectVersion(recordingId: String, versionId: String)
    suspend fun adjustment(versionId: String): AdjustmentEntity
    suspend fun adjust(value: AdjustmentEntity)
}

class LibraryRepository(private val database: LibraryDatabase) : LyricsStore, ReaderStore, TranslationStore {
    private val dao = database.libraryDao()
    private val json = Json { ignoreUnknownKeys = true }
    val entries = dao.observeLibrary().map { rows -> rows.map { row ->
        LibraryEntry(row.recording.toModel(), row.recording.lastPlayedAtMs, row.recording.playCount, row.hasLyrics, row.recording.lookupStatus, row.recording.favorite)
    } }

    suspend fun resolveRecording(incoming: Recording, previousId: String? = null): Recording = database.withTransaction {
        val previous = previousId?.let { dao.recording(it) }
            ?.takeIf { RecordingIdentity.sameObservation(it.toModel(), incoming) }
        val candidates = if (previous == null) dao.candidates(RecordingIdentity.normalize(incoming.title))
            .filter { RecordingIdentity.compatible(it.toModel(), incoming) } else emptyList()
        val imported = if (previous == null && candidates.isEmpty()) {
            FolderIdentity.match(dao.folderRecordings().map { it.toModel() }, incoming)?.let { dao.recording(it.id) }
        } else null
        val existing = previous ?: candidates.singleOrNull() ?: imported
        val result = if (existing != null) RecordingIdentity.enrich(existing.toModel(), incoming)
            else incoming.copy(id = UUID.randomUUID().toString())
        dao.upsert(result.toEntity(existing))
        result
    }

    suspend fun recording(id: String): Recording? = dao.recording(id)?.toModel()
    suspend fun folderHasLyrics(incoming: Recording): Boolean {
        val match = FolderIdentity.match(dao.allRecordings().map { it.toModel() }, incoming) ?: return false
        return dao.versions(match.id).isNotEmpty()
    }

    /** Recheck inside the same transaction as insertion, including concurrent playback collection. */
    suspend fun saveFolderMatch(incoming: Recording, candidates: List<LyricVersion>): Boolean = database.withTransaction {
        require(candidates.isNotEmpty() && candidates.all { v -> v.recordingId == incoming.id && v.lines.any { it.text.isNotBlank() } })
        val rows = dao.allRecordings()
        val match = FolderIdentity.match(rows.map { it.toModel() }, incoming)
        if (match != null && dao.versions(match.id).isNotEmpty()) return@withTransaction false
        val recording = match?.copy(fileName = match.fileName ?: incoming.fileName)
            ?: incoming.copy(id = UUID.randomUUID().toString())
        dao.upsert(recording.toEntity(rows.firstOrNull { it.id == recording.id }))
        val versions = candidates.distinctBy { it.contentHash }.map { version ->
            version.copy(recordingId = recording.id, id = UUID.nameUUIDFromBytes("${recording.id}:${version.contentHash}".toByteArray(Charsets.UTF_8)).toString())
        }
        saveCandidates(recording, versions)
        true
    }
    override suspend fun translation(versionId: String): TranslationRecord? = dao.translation(versionId)?.let { json.decodeFromString(it.contentJson) }
    override suspend fun saveTranslation(record: TranslationRecord) = database.withTransaction {
        // Clearing downloads can win a race with a provider response. Do not resurrect it.
        val stored = dao.version(record.versionId) ?: return@withTransaction
        val version = json.decodeFromString<LyricVersion>(stored.contentJson)
        require(TranslationRepository.valid(version, record))
        dao.translation(TranslationEntity(record.versionId, json.encodeToString(record)))
    }
    suspend fun favorite(id: String, value: Boolean) = dao.favorite(id, value)
    suspend fun clearHistory() = dao.clearHistory()
    suspend fun savePersonal(version: LyricVersion) = database.withTransaction {
        require(version.origin != LyricOrigin.PROVIDER)
        requireNotNull(dao.recording(version.recordingId))
        dao.upsertLyrics(LyricEntity(version.id, version.recordingId, json.encodeToString(version)))
        dao.select(SelectionEntity(version.recordingId, version.id))
        dao.lookupStatus(version.recordingId, "available")
    }
    suspend fun clearDownloads(preserveFavorites: Boolean = true) = database.withTransaction {
        dao.allRecordings().filterNot { preserveFavorites && it.favorite }.forEach { recording ->
            versions(recording.id).filter { it.origin == LyricOrigin.PROVIDER }.forEach { dao.deleteVersion(it.id) }
            val remaining = versions(recording.id)
            if (dao.selected(recording.id) == null) remaining.firstOrNull()?.let { dao.select(SelectionEntity(recording.id, it.id)) }
            dao.lookupStatus(recording.id, if (remaining.isEmpty()) "not_requested" else "available")
        }
    }
    suspend fun lyricReferences(recordingId: String): List<LyricReference> = dao.references(recordingId).map { json.decodeFromString(it.contentJson) }

    suspend fun exportBackup(options: BackupOptions = BackupOptions()): BackupDocument = database.withTransaction {
        require(options.hasContent) { "Choose at least one backup section." }
        if (options.lyrics == LyricsBackupMode.NONE && !options.songMetadata && !options.history)
            return@withTransaction BackupDocument(songs = emptyList(), options = options)
        val fullLyrics = options.lyrics == LyricsBackupMode.FULL
        BackupDocument(songs = dao.allRecordings().map { r ->
            val versions = if (options.lyrics != LyricsBackupMode.NONE) versions(r.id) else emptyList()
            BackupSong(r.toModel(), r.playCount, r.lastPlayedAtMs, r.favorite, versions, if (options.lyrics != LyricsBackupMode.NONE) dao.selected(r.id)?.id else null,
                if (fullLyrics) versions.map { adjustment(it.id) }.map { BackupAdjustment(it.versionId, it.delayMs, it.allowUncertainSync) } else emptyList(),
                if (fullLyrics) versions.mapNotNull { translation(it.id) } else emptyList(),
                if (options.lyrics != LyricsBackupMode.NONE) lyricReferences(r.id).filter { ref -> versions.none { LyricReference.from(it) == ref } } else emptyList())
        }).select(options)
    }
    /** Validate before opening the transaction. Existing selections and adjustments always win. */
    suspend fun restore(document: BackupDocument): Int {
        BackupCodec.validate(document)
        return database.withTransaction {
            var added = 0
            val preexistingIds = dao.allRecordings().mapTo(mutableSetOf()) { it.id }
            val sourceIds = document.songs.mapTo(mutableSetOf()) { it.recording.id }
            val claimedTargets = mutableSetOf<String>()
            document.songs.forEach { song ->
                val originalId = song.recording.id
                val byId = dao.recording(originalId)
                // Partial identities lack the album/duration needed for a safe fuzzy match. Full
                // imports may reuse only unclaimed rows from before this transaction, reserving
                // every incoming stable ID for its own recording even if it appears later.
                val matching = if (byId == null && document.options.songMetadata)
                    dao.candidates(RecordingIdentity.normalize(song.recording.title)).filter {
                        it.id in preexistingIds && it.id !in sourceIds && it.id !in claimedTargets &&
                            RecordingIdentity.compatible(it.toModel(), song.recording)
                    }.singleOrNull() else null
                val targetId = matching?.id ?: if (byId != null && !RecordingIdentity.compatible(byId.toModel(), song.recording))
                    UUID.nameUUIDFromBytes(json.encodeToString(song.recording).toByteArray()).toString() else originalId
                claimedTargets.add(targetId)
                val existing = dao.recording(targetId)
                if (existing == null) added++
                val model = song.recording.copy(id = targetId)
                val row = if (existing != null && document.options.songMetadata) existing.copy(
                    album = existing.album?.takeIf { it.isNotBlank() } ?: model.album,
                    durationMs = existing.durationMs ?: model.durationMs,
                    versionLabel = existing.versionLabel?.takeIf { it.isNotBlank() } ?: model.versionLabel,
                    fileName = existing.fileName?.takeIf { it.isNotBlank() } ?: model.fileName,
                ) else existing ?: model.toEntity(null)
                dao.upsert(row.copy(playCount = if (document.options.history) maxOf(row.playCount, song.plays) else row.playCount,
                    lastPlayedAtMs = if (document.options.history) listOfNotNull(row.lastPlayedAtMs, song.lastPlayed).maxOrNull() else row.lastPlayedAtMs,
                    favorite = if (existing != null || !document.options.songMetadata) row.favorite else song.favorite))
                song.lyricReferences.forEach { source ->
                    var reference = source.copy(recordingId = targetId)
                    val conflict = dao.reference(reference.id)
                    val fullConflict = dao.version(reference.id)?.let { LyricReference.from(json.decodeFromString<LyricVersion>(it.contentJson)) != reference } ?: false
                    if (fullConflict || (conflict != null && json.decodeFromString<LyricReference>(conflict.contentJson) != reference))
                        reference = reference.copy(id = UUID.nameUUIDFromBytes(json.encodeToString(reference).toByteArray(Charsets.UTF_8)).toString())
                    if (dao.insertReference(LyricReferenceEntity(reference.id, targetId, json.encodeToString(reference))) != -1L) added++
                }
                val mapped = mutableMapOf<String, String>()
                song.versions.forEach { source ->
                    var version = source.copy(recordingId = targetId)
                    val conflict = dao.version(version.id)
                    val referenceConflict = dao.reference(version.id)?.let { json.decodeFromString<LyricReference>(it.contentJson) != LyricReference.from(version) } ?: false
                    if (referenceConflict || (conflict != null && json.decodeFromString<LyricVersion>(conflict.contentJson) != version))
                        version = version.copy(id = UUID.nameUUIDFromBytes(json.encodeToString(version).toByteArray()).toString())
                    mapped[source.id] = version.id
                    if (dao.version(version.id) == null) { dao.upsertLyrics(LyricEntity(version.id, targetId, json.encodeToString(version))); added++ }
                }
                if (dao.selected(targetId) == null) song.selected?.let { mapped[it] }?.let { dao.select(SelectionEntity(targetId, it)) }
                song.adjustments.forEach { a -> mapped[a.versionId]?.let { id ->
                    if (dao.adjustment(id) == null) dao.adjust(AdjustmentEntity(id, a.delayMs, a.allowUncertainSync))
                } }
                song.translations.forEach { t -> mapped[t.versionId]?.let { id ->
                    if (dao.translation(id) == null) saveTranslation(t.copy(versionId = id))
                } }
                if (versions(targetId).isNotEmpty()) dao.lookupStatus(targetId, "available")
            }
            added
        }
    }

    suspend fun recordQualifiedPlay(recordingId: String, occurrenceId: String, playedAtMs: Long) = database.withTransaction {
        if (dao.insertOccurrence(OccurrenceEntity(occurrenceId, recordingId)) != -1L) dao.incrementPlay(recordingId, playedAtMs)
    }

    override suspend fun selectedVersion(recordingId: String): LyricVersion? = dao.selected(recordingId)?.let { json.decodeFromString<LyricVersion>(it.contentJson) }
    override suspend fun versions(recordingId: String): List<LyricVersion> = com.kmab.singalong.lyrics.LyricPreference.sorted(
        dao.versions(recordingId).map { json.decodeFromString(it.contentJson) }, dao.recording(recordingId)?.durationMs)

    override suspend fun saveCandidates(recording: Recording, versions: List<LyricVersion>) = database.withTransaction {
        require(versions.all { it.recordingId == recording.id })
        versions.forEach { dao.upsertLyrics(LyricEntity(it.id, it.recordingId, json.encodeToString(it))) }
        // Preserve every explicit selection, including a personal version created in a later phase.
        if (dao.selected(recording.id) == null) com.kmab.singalong.lyrics.LyricPreference.sorted(versions, recording.durationMs)
            .firstOrNull()?.let { dao.select(SelectionEntity(recording.id, it.id)) }
        dao.lookupStatus(recording.id, "available")
    }

    override suspend fun selectVersion(recordingId: String, versionId: String) = database.withTransaction {
        val version = requireNotNull(dao.version(versionId))
        require(version.recordingId == recordingId)
        dao.select(SelectionEntity(recordingId, versionId))
    }

    override suspend fun setLookupStatus(recordingId: String, status: String) = dao.lookupStatus(recordingId, status)
    override suspend fun storedStatus(recordingId: String): String? = dao.recording(recordingId)?.lookupStatus
    override suspend fun adjustment(versionId: String): AdjustmentEntity = dao.adjustment(versionId) ?: AdjustmentEntity(versionId)
    override suspend fun adjust(value: AdjustmentEntity) = database.withTransaction {
        require(value.delayMs in -600_000..600_000)
        if (dao.version(value.versionId) == null) return@withTransaction
        dao.adjust(value)
    }

    private fun RecordingEntity.toModel() = Recording(id, title, json.decodeFromString(artistsJson), album, durationMs, versionLabel, fileName)
    private fun Recording.toEntity(existing: RecordingEntity?) = RecordingEntity(
        id, title, RecordingIdentity.normalize(title), json.encodeToString(artists), album, durationMs, versionLabel,
        existing?.lastPlayedAtMs, existing?.playCount ?: 0, existing?.lookupStatus ?: "not_requested", existing?.favorite ?: false,
        fileName ?: existing?.fileName,
    )
}
