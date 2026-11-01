package com.kmab.singalong.library

import androidx.room.withTransaction
import com.kmab.singalong.model.*
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

interface LyricsStore {
    suspend fun selectedVersion(recordingId: String): LyricVersion?
    suspend fun saveCandidates(recording: Recording, versions: List<LyricVersion>)
    suspend fun setLookupStatus(recordingId: String, status: String)
    suspend fun storedStatus(recordingId: String): String? = null
}

interface ReaderStore {
    suspend fun versions(recordingId: String): List<LyricVersion>
    suspend fun selectVersion(recordingId: String, versionId: String)
    suspend fun adjustment(versionId: String): AdjustmentEntity
    suspend fun adjust(value: AdjustmentEntity)
}

class LibraryRepository(private val database: LibraryDatabase) : LyricsStore, ReaderStore {
    private val dao = database.libraryDao()
    private val json = Json { ignoreUnknownKeys = true }
    val entries = dao.observeLibrary().map { rows -> rows.map { row ->
        LibraryEntry(row.recording.toModel(), row.recording.lastPlayedAtMs, row.recording.playCount, row.hasLyrics, row.recording.lookupStatus)
    } }

    suspend fun resolveRecording(incoming: Recording, previousId: String? = null): Recording = database.withTransaction {
        val previous = previousId?.let { dao.recording(it) }
            ?.takeIf { RecordingIdentity.sameObservation(it.toModel(), incoming) }
        val candidates = if (previous == null) dao.candidates(RecordingIdentity.normalize(incoming.title))
            .filter { RecordingIdentity.compatible(it.toModel(), incoming) } else emptyList()
        val existing = previous ?: candidates.singleOrNull()
        val result = if (existing != null) RecordingIdentity.enrich(existing.toModel(), incoming)
            else incoming.copy(id = UUID.randomUUID().toString())
        dao.upsert(result.toEntity(existing))
        result
    }

    suspend fun recording(id: String): Recording? = dao.recording(id)?.toModel()

    suspend fun recordQualifiedPlay(recordingId: String, occurrenceId: String, playedAtMs: Long) = database.withTransaction {
        if (dao.insertOccurrence(OccurrenceEntity(occurrenceId, recordingId)) != -1L) dao.incrementPlay(recordingId, playedAtMs)
    }

    override suspend fun selectedVersion(recordingId: String): LyricVersion? = dao.selected(recordingId)?.let { json.decodeFromString<LyricVersion>(it.contentJson) }
    override suspend fun versions(recordingId: String): List<LyricVersion> = dao.versions(recordingId).map { json.decodeFromString(it.contentJson) }

    override suspend fun saveCandidates(recording: Recording, versions: List<LyricVersion>) = database.withTransaction {
        require(versions.all { it.recordingId == recording.id })
        versions.forEach { dao.upsertLyrics(LyricEntity(it.id, it.recordingId, json.encodeToString(it))) }
        // Preserve every explicit selection, including a personal version created in a later phase.
        if (dao.selected(recording.id) == null) versions.firstOrNull()?.let { dao.select(SelectionEntity(recording.id, it.id)) }
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
    override suspend fun adjust(value: AdjustmentEntity) {
        require(value.delayMs in -600_000..600_000)
        dao.adjust(value)
    }

    private fun RecordingEntity.toModel() = Recording(id, title, json.decodeFromString(artistsJson), album, durationMs, versionLabel)
    private fun Recording.toEntity(existing: RecordingEntity?) = RecordingEntity(
        id, title, RecordingIdentity.normalize(title), json.encodeToString(artists), album, durationMs, versionLabel,
        existing?.lastPlayedAtMs, existing?.playCount ?: 0, existing?.lookupStatus ?: "not_requested",
    )
}
