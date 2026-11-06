package com.kmab.singalong.library

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.kmab.singalong.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class LibraryRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, LibraryDatabase::class.java).build()
    private val repository = LibraryRepository(db)
    @After fun closeDatabase() { db.close() }
    @Test fun lateTranslationAndAdjustmentAfterClearingDownloadsAreDiscarded() = runTest {
        val song = repository.resolveRecording(Recording("raw", "Song", listOf("Artist"), null, null))
        val version = LyricVersion("provider", song.id, "hash", LyricOrigin.PROVIDER,
            listOf(LyricLine("line", null, "Original words")), "Test source", null, MatchConfidence.CONFIDENT)
        repository.saveCandidates(song, listOf(version))
        val translation = com.kmab.singalong.translation.TranslationRecord(version.id, version.contentHash,
            "test", "fr", mapOf("line" to "Translated words"), "Test attribution")
        repository.saveTranslation(translation)
        repository.adjust(AdjustmentEntity(version.id, 50))
        assertNotNull(repository.translation(version.id))
        repository.clearDownloads()
        repository.saveTranslation(translation)
        repository.adjust(AdjustmentEntity(version.id, 100))
        assertNull(db.libraryDao().version(version.id))
        assertNull(repository.translation(version.id))
        assertNull(db.libraryDao().adjustment(version.id))
        assertTrue(repository.versions(song.id).isEmpty())
    }
    @Test fun filenameSearchHintsSurvivePersistenceAndDeduplicateTaggedCopies() = runTest {
        val song = Recording("file", "Song", emptyList(), "Album", 120_000, fileName = "Artist - Song.mp3")
        val version = LyricVersion("provider", song.id, "hash", LyricOrigin.PROVIDER,
            listOf(LyricLine("1", null, "Synthetic words")), "Test source", null, MatchConfidence.CONFIDENT)
        assertTrue(repository.saveFolderMatch(song, listOf(version)))
        val saved = repository.entries.first().single().recording
        assertEquals(song.fileName, saved.fileName)
        assertTrue(saved.artists.isEmpty())
        assertTrue(repository.folderHasLyrics(song.copy(id = "another", artists = listOf("Artist"), fileName = null)))
        val playing = repository.resolveRecording(song.copy(id = "player", artists = listOf("Artist"), fileName = null))
        assertEquals(saved.id, playing.id)
        assertNotNull(repository.selectedVersion(playing.id))
        assertEquals(1, repository.entries.first().size)
    }
    @Test fun folderImportDeduplicatesFilesAndPreservesSelectedPersonalLyricsAndHistory() = runTest {
        val song = Recording("file-one", "Song", listOf("Artist"), "Album", 120_000)
        val version = LyricVersion("provider", song.id, "content", LyricOrigin.PROVIDER,
            listOf(LyricLine("1", 0, "Synthetic words")), "Test source", null, MatchConfidence.CONFIDENT)
        assertTrue(repository.saveFolderMatch(song, listOf(version, version.copy(id = "same-content"))))
        val entry = repository.entries.first().single()
        assertEquals(0, entry.playCount.toInt()); assertNull(entry.lastPlayedAtMs)
        assertEquals(1, repository.versions(entry.recording.id).size)
        val personal = version.copy(id = "personal", recordingId = entry.recording.id, origin = LyricOrigin.PERSONAL, contentHash = "personal-hash")
        repository.savePersonal(personal)
        val copy = song.copy(id = "file-two", title = "01 - Artist - Song.mp3", artists = emptyList(), album = null)
        assertTrue(repository.folderHasLyrics(copy))
        assertFalse(repository.saveFolderMatch(copy, listOf(version.copy(recordingId = copy.id))))
        assertEquals(1, repository.entries.first().size)
        assertEquals("personal", repository.selectedVersion(entry.recording.id)?.id)
    }

    @Test fun qualifiedOccurrencesAreIdempotent() = runTest {
        val song = repository.resolveRecording(Recording("raw", "Song", listOf("Artist"), null, 120_000))
        repository.recordQualifiedPlay(song.id, "one", 10)
        repository.recordQualifiedPlay(song.id, "one", 10)
        repository.recordQualifiedPlay(song.id, "two", 20)
        val entry = repository.entries.first().single()
        assertEquals(2L, entry.playCount)
        assertEquals(20L, entry.lastPlayedAtMs)
    }

    @Test fun enrichmentKeepsIdentityAndDifferentRecordingsRemainSeparate() = runTest {
        val first = repository.resolveRecording(Recording("raw", "Song", listOf("Artist"), null, null))
        val enriched = repository.resolveRecording(first.copy(album = "Album", durationMs = 120_000), first.id)
        assertEquals(first.id, enriched.id)
        val live = repository.resolveRecording(first.copy(title = "Song (Live)", durationMs = 180_000))
        assertNotEquals(first.id, live.id)
    }

    @Test fun newProviderResultNeverOverridesSelectedPersonalVersion() = runTest {
        val song = repository.resolveRecording(Recording("raw", "Song", listOf("Artist"), null, null))
        val personal = LyricVersion("personal", song.id, "hash1", LyricOrigin.PERSONAL, listOf(LyricLine("line", null, "My own words")), null, null, MatchConfidence.CONFIDENT)
        repository.saveCandidates(song, listOf(personal))
        repository.saveCandidates(song, listOf(personal.copy(id = "provider", origin = LyricOrigin.PROVIDER, contentHash = "hash2")))
        assertEquals("personal", repository.selectedVersion(song.id)?.id)
    }
    @Test fun backupRestoreIsIdempotentAndInvalidBackupDoesNotMutate() = runTest {
        val song = repository.resolveRecording(Recording("raw", "Backup fixture", listOf("Artist"), null, null))
        repository.recordQualifiedPlay(song.id, "one", 10)
        val personal = com.kmab.singalong.editor.LyricsEditor.import(song.id, "Synthetic original")
        repository.savePersonal(personal)
        val document = repository.exportBackup()
        assertEquals(0, repository.restore(document))
        assertEquals(0, repository.restore(document))
        assertEquals(document, repository.exportBackup())
        try { repository.restore(document.copy(schema = 99)); fail("Expected validation failure") } catch (_: IllegalArgumentException) { }
        assertEquals(document, repository.exportBackup())
    }
    @Test fun clearingDownloadsPreservesPersonalVersionsAndFavorites() = runTest {
        val song = repository.resolveRecording(Recording("raw", "Cleanup fixture", listOf("Artist"), null, null))
        val personal = com.kmab.singalong.editor.LyricsEditor.import(song.id, "Synthetic original")
        repository.savePersonal(personal)
        repository.saveCandidates(song, listOf(personal.copy(id = "download", origin = LyricOrigin.PROVIDER)))
        repository.favorite(song.id, true)
        repository.clearDownloads()
        assertEquals(2, repository.versions(song.id).size)
        repository.favorite(song.id, false)
        repository.clearDownloads()
        assertEquals(listOf(personal), repository.versions(song.id))
        repository.clearHistory()
        assertEquals(personal, repository.selectedVersion(song.id))
    }
    @Test fun failedRestoreRollsBackEarlierRecordingWrites() = runTest {
        val before = repository.exportBackup()
        val recording = Recording("restore", "Rollback fixture", listOf("Artist"), null, null)
        val version = com.kmab.singalong.editor.LyricsEditor.import(recording.id, "Synthetic words")
        val backup = com.kmab.singalong.backup.BackupDocument(songs = listOf(
            com.kmab.singalong.backup.BackupSong(recording, 1, 1, false, listOf(version), version.id)))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_restore BEFORE INSERT ON lyrics BEGIN SELECT RAISE(ABORT, 'injected restore failure'); END")
        try { repository.restore(backup); fail("Expected injected failure") } catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals(before, repository.exportBackup())
    }
}
