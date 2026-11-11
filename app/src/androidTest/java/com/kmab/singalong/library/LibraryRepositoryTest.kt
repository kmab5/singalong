package com.kmab.singalong.library

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.kmab.singalong.model.*
import com.kmab.singalong.backup.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class LibraryRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, LibraryDatabase::class.java).build()
    private val repository = LibraryRepository(db)
    @After fun closeDatabase() { db.close() }
    @Test fun preferredLinksExportHonorsSavedPersonalSelectionWithoutMutatingLibrary() = runTest {
        val recording = repository.resolveRecording(Recording("raw", "Preferred export", listOf("Artist"), null, 120000))
        val timed = LyricVersion("timed", recording.id, "timed-hash", LyricOrigin.PROVIDER,
            listOf(LyricLine("line", 1000, "Provider words")), "Source", "https://example.com/song", MatchConfidence.CONFIDENT,
            providerDurationMs = 120000)
        repository.saveCandidates(recording, listOf(timed))
        val personal = com.kmab.singalong.editor.LyricsEditor.import(recording.id, "Personal untimed words")
        repository.savePersonal(personal)
        repository.adjust(AdjustmentEntity(personal.id, 150))
        val before = repository.exportBackup()
        val options = BackupOptions(lyrics = LyricsBackupMode.LINKS_METADATA, versionScope = LyricVersionScope.PREFERRED)
        val links = repository.exportBackup(options)
        assertEquals(listOf(personal.id), links.songs.single().lyricReferences.map { it.id })
        assertTrue(links.songs.single().versions.isEmpty())
        assertEquals(links, links.select(options))
        val full = repository.exportBackup(options.copy(lyrics = LyricsBackupMode.FULL))
        assertEquals(listOf(personal), full.songs.single().versions)
        assertEquals(listOf(personal.id), full.songs.single().adjustments.map { it.versionId })
        assertEquals(before, repository.exportBackup())
        assertEquals(personal.id, repository.selectedVersion(recording.id)?.id)
    }
    @Test fun partialRestoreKeepsSameTitleRecordingsHistoryAndSelectionsSeparate() = runTest {
        val songs = listOf("first" to "Album A", "second" to "Album B").mapIndexed { index, (id, album) ->
            val recording = Recording(id, "Same title", listOf("Artist"), album, 120000L + index * 30000L)
            val version = com.kmab.singalong.editor.LyricsEditor.import(id, "Original words $id")
            BackupSong(recording, index + 2L, index + 100L, false, listOf(version), version.id)
        }
        val partial = BackupDocument(songs = songs).select(BackupOptions(LyricsBackupMode.FULL, false, true, false))
        assertEquals(4, repository.restore(partial))
        assertEquals(2, repository.entries.first().size)
        songs.forEach { song ->
            val row = requireNotNull(db.libraryDao().recording(song.recording.id))
            assertEquals(song.plays, row.playCount)
            assertEquals(song.lastPlayed, row.lastPlayedAtMs)
            assertEquals(song.selected, repository.selectedVersion(song.recording.id)?.id)
        }
        val after = repository.exportBackup()
        assertEquals(0, repository.restore(partial))
        assertEquals(after, repository.exportBackup())
    }
    @Test fun fullRestoreCannotFuzzyMatchRowsCreatedByTheSameImport() = runTest {
        val songs = listOf("first", "second").map { id ->
            BackupSong(Recording(id, "Same title", listOf("Artist"), null, null), 1, 100, false, emptyList(), null)
        }
        val backup = BackupDocument(songs = songs)
        assertEquals(2, repository.restore(backup))
        assertEquals(setOf("first", "second"), repository.entries.first().map { it.recording.id }.toSet())
        assertEquals(0, repository.restore(backup))
        assertEquals(2, repository.entries.first().size)
    }
    @Test fun fullRestoreClaimsPreexistingMatchesOnlyOnceAndRemainsIdempotent() = runTest {
        val existing = repository.resolveRecording(Recording("raw", "Same title", listOf("Artist"), null, null))
        val backup = BackupDocument(songs = listOf("first", "second").mapIndexed { index, id ->
            BackupSong(Recording(id, "Same title", listOf("Artist"), null, null), index + 1L, 100, false, emptyList(), null)
        })
        assertEquals(1, repository.restore(backup))
        assertEquals(setOf(existing.id, "second"), repository.entries.first().map { it.recording.id }.toSet())
        val after = repository.exportBackup()
        assertEquals(0, repository.restore(backup))
        assertEquals(after, repository.exportBackup())
    }
    @Test fun metadataOnlyRestoreRetainsLinksWithoutClaimingLyricsAndIsIdempotent() = runTest {
        val recording = Recording("linked", "Linked song", listOf("Artist"), "Album", 123000)
        val version = LyricVersion("linked-version", recording.id, "content", LyricOrigin.PROVIDER,
            listOf(LyricLine("line", null, "Original words")), "Provider", "https://example.com/song", MatchConfidence.CONFIDENT)
        val backup = BackupDocument(songs = listOf(BackupSong(recording, 3, 100, true, listOf(version), version.id)))
            .select(BackupOptions(LyricsBackupMode.LINKS_METADATA))
        assertEquals(2, repository.restore(backup))
        assertEquals(0, repository.restore(backup))
        val entry = repository.entries.first().single()
        assertFalse(entry.hasLyrics)
        assertEquals(3L, entry.playCount)
        assertTrue(repository.versions(recording.id).isEmpty())
        assertNull(repository.selectedVersion(recording.id))
        assertEquals("https://example.com/song", repository.lyricReferences(recording.id).single().sourceUrl)
        val exported = repository.exportBackup(backup.options)
        assertEquals(backup, exported)
    }
    @Test fun omittedSectionsPreserveExistingHistoryMetadataLyricsAndSelection() = runTest {
        val song = repository.resolveRecording(Recording("raw", "Selective fixture", listOf("Artist"), "Album", 200000))
        repository.recordQualifiedPlay(song.id, "play", 100)
        repository.favorite(song.id, true)
        val personal = com.kmab.singalong.editor.LyricsEditor.import(song.id, "My words")
        repository.savePersonal(personal)
        repository.adjust(AdjustmentEntity(personal.id, 150))
        val before = repository.exportBackup()
        val metadata = before.select(BackupOptions(LyricsBackupMode.NONE, true, false, false))
        assertEquals(0, repository.restore(metadata))
        val lyrics = before.select(BackupOptions(LyricsBackupMode.FULL, false, false, false))
        assertEquals(0, repository.restore(lyrics))
        assertEquals(before, repository.exportBackup())
        val settingsOnly = BackupDocument(songs = emptyList(), preferences = mapOf("theme" to "dark"),
            options = BackupOptions(LyricsBackupMode.NONE, false, false, true))
        assertEquals(0, repository.restore(settingsOnly))
        assertEquals(before, repository.exportBackup())
    }
    @Test fun referenceFailureRollsBackRecordingAndHistoryChanges() = runTest {
        val before = repository.exportBackup()
        val recording = Recording("rollback-reference", "Reference fixture", emptyList(), null, null)
        val reference = LyricReference("reference", recording.id, "hash", LyricOrigin.PROVIDER, "Source",
            "https://example.com/lyrics", MatchConfidence.CONFIDENT)
        val backup = BackupDocument(songs = listOf(BackupSong(recording, 1, 1, false, emptyList(), null, lyricReferences = listOf(reference))),
            options = BackupOptions(lyrics = LyricsBackupMode.LINKS_METADATA))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_reference BEFORE INSERT ON lyric_references BEGIN SELECT RAISE(ABORT, 'injected restore failure'); END")
        try { repository.restore(backup); fail("Expected injected failure") } catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals(before, repository.exportBackup())
    }
    @Test fun fullMetadataEnrichesEarlierLinksOnlyRestoreWithoutReplacingPresentValues() = runTest {
        val recording = Recording("enriched", "Song", listOf("Artist"), "Original album", 123000,
            versionLabel = "Original label", fileName = "original.mp3")
        val version = LyricVersion("enriched-lyrics", recording.id, "hash", LyricOrigin.PROVIDER,
            listOf(LyricLine("line", null, "Original words")), "Source", "https://example.com/song", MatchConfidence.CONFIDENT)
        val source = BackupDocument(songs = listOf(BackupSong(recording, 4, 100, false, listOf(version), version.id)))
        repository.restore(source.select(BackupOptions(LyricsBackupMode.LINKS_METADATA, false, false, false)))
        assertNull(repository.recording(recording.id)?.album)
        val row = requireNotNull(db.libraryDao().recording(recording.id))
        db.libraryDao().upsert(row.copy(fileName = "current.mp3", versionLabel = "Current label"))
        val full = source.select(BackupOptions(LyricsBackupMode.FULL, true, false, false))
        repository.restore(full)
        assertEquals("Original album", repository.recording(recording.id)?.album)
        assertEquals(123000L, repository.recording(recording.id)?.durationMs)
        assertEquals("current.mp3", repository.recording(recording.id)?.fileName)
        assertEquals("Current label", repository.recording(recording.id)?.versionLabel)
        val after = repository.exportBackup()
        assertEquals(0, repository.restore(full))
        assertEquals(after, repository.exportBackup())
    }
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
