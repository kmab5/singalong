package com.kmab.singalong.backup

import com.kmab.singalong.model.*
import com.kmab.singalong.translation.TranslationRecord
import java.io.ByteArrayOutputStream
import org.junit.Assert.*
import org.junit.Test

class BackupVersionScopeTest {
    private val recording = Recording("song", "Song", listOf("Artist"), null, 120000)
    private val personal = LyricVersion("personal", recording.id, "personal-hash", LyricOrigin.PERSONAL,
        listOf(LyricLine("line", null, "Personal words")), null, null, MatchConfidence.CONFIDENT)
    private val close = personal.copy(id = "timed-close", contentHash = "close-hash", origin = LyricOrigin.PROVIDER,
        lines = listOf(LyricLine("line", 1000, "Provider words")), sourceName = "Source", sourceUrl = "https://example.com/close", providerDurationMs = 121000)
    private val far = close.copy(id = "timed-far", contentHash = "far-hash", providerDurationMs = 150000)
    private val versions = listOf(far, personal, close)
    private val song = BackupSong(recording, 2, 100, true, versions, personal.id,
        adjustments = versions.map { BackupAdjustment(it.id, 50, false) },
        translations = versions.map { TranslationRecord(it.id, it.contentHash, "test", "fr", mapOf("line" to "Translated words"), "Attribution") },
        lyricReferences = listOf(LyricReference.from(far), LyricReference.from(personal)))
    private val document = BackupDocument(songs = listOf(song))
    private val preferred = BackupOptions(versionScope = LyricVersionScope.PREFERRED)

    @Test fun selectedPersonalVersionWinsAndOnlyItsAttachmentsRemain() {
        val selected = document.select(preferred).songs.single()
        assertEquals(listOf(personal), selected.versions)
        assertEquals(personal.id, selected.selected)
        assertEquals(listOf(personal.id), selected.adjustments.map { it.versionId })
        assertEquals(listOf(personal.id), selected.translations.map { it.versionId })
        assertEquals(listOf(personal.id), selected.lyricReferences.map { it.id })
        assertEquals(3, document.songs.single().versions.size)
        assertEquals(personal.id, document.songs.single().selected)
    }
    @Test fun missingSelectionUsesTimedVersionWithClosestDuration() {
        val selected = document.copy(songs = listOf(song.copy(selected = null))).select(preferred).songs.single()
        assertEquals(listOf(close), selected.versions)
        assertEquals(listOf(close.id), selected.adjustments.map { it.versionId })
        assertEquals(listOf(close.id), selected.translations.map { it.versionId })
        assertTrue(selected.lyricReferences.isEmpty())
    }
    @Test fun linksOnlyHonorsSelectionBeforeDiscardingLyricContent() {
        val selected = document.select(preferred.copy(lyrics = LyricsBackupMode.LINKS_METADATA)).songs.single()
        assertTrue(selected.versions.isEmpty())
        assertEquals(listOf(LyricReference.from(personal)), selected.lyricReferences)
        assertTrue(selected.adjustments.isEmpty()); assertTrue(selected.translations.isEmpty())
        assertNull(selected.selected)
    }
    @Test fun referenceOnlyFallbackUsesConfidenceThenDurationThenStableId() {
        val closeRef = LyricReference.from(close)
        val references = listOf(closeRef.copy(id = "uncertain", confidence = MatchConfidence.UNCERTAIN, providerDurationMs = 120000),
            closeRef.copy(id = "far", providerDurationMs = 150000), closeRef.copy(id = "b"), closeRef.copy(id = "a"))
        val source = document.copy(songs = listOf(song.copy(versions = emptyList(), selected = null, adjustments = emptyList(), translations = emptyList(), lyricReferences = references)))
        val selected = source.select(preferred).songs.single()
        assertTrue(selected.versions.isEmpty()); assertNull(selected.selected)
        assertEquals(listOf("a"), selected.lyricReferences.map { it.id })
    }
    @Test fun preferredSelectionIsIdempotentAndCannotBeBroadenedToAll() {
        listOf(LyricsBackupMode.FULL, LyricsBackupMode.LINKS_METADATA).forEach { mode ->
            val options = preferred.copy(lyrics = mode, songMetadata = false)
            val selected = document.select(options)
            assertEquals(selected, selected.select(options))
            assertThrows(IllegalArgumentException::class.java) { selected.select(options.copy(versionScope = LyricVersionScope.ALL)) }
            val bytes = ByteArrayOutputStream().also { BackupArchive.write(selected, it) }.toByteArray()
            assertEquals(selected, BackupArchive.read(bytes.inputStream()))
        }
    }
    @Test fun noLyricsIgnoresVersionScopeAndAllRetainsEveryVersion() {
        assertEquals(document, document.select(BackupOptions()))
        val source = document.select(preferred)
        val withoutLyrics = source.select(preferred.copy(lyrics = LyricsBackupMode.NONE, versionScope = LyricVersionScope.ALL))
        assertTrue(withoutLyrics.songs.single().versions.isEmpty())
        assertTrue(withoutLyrics.songs.single().lyricReferences.isEmpty())
    }
    @Test fun legacyDefaultsToAllAndPreferredDeclarationCannotContainMultipleVersions() {
        assertEquals(LyricVersionScope.ALL, BackupCodec.decode("""{"schema":1,"songs":[]}""").options.versionScope)
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.validate(document.copy(options = preferred)) }
    }
}
