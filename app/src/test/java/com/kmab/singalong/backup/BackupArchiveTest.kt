package com.kmab.singalong.backup

import com.kmab.singalong.model.*
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class BackupArchiveTest {
    private val recording = Recording("song", "Song", listOf("Artist"), "Album", 123000, fileName = "song.mp3")
    private val version = LyricVersion("version/unsafe", "song", "hash", LyricOrigin.PROVIDER,
        listOf(LyricLine("line", 1000, "Original words")), "Provider", "https://example.com/song",
        MatchConfidence.CONFIDENT, providerId = "source-id", attribution = "Credit")
    private val document = BackupDocument(songs = listOf(BackupSong(recording, 4, 99, true, listOf(version), version.id)),
        preferences = mapOf("theme" to "dark"))

    private fun archive(options: BackupOptions = BackupOptions()): ByteArray = ByteArrayOutputStream().also {
        BackupArchive.write(document, it, options)
    }.toByteArray()
    private fun entries(bytes: ByteArray): Map<String, ByteArray> = buildMap {
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) { val entry = zip.nextEntry ?: break; put(entry.name, zip.readBytes()) }
        }
    }
    private fun zip(entries: Map<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { output ->
        ZipOutputStream(output).use { zip -> entries.forEach { (name, bytes) ->
            zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
        } }
    }.toByteArray()

    @Test fun fullArchiveRoundTripsOriginalContentsAndUsesSafePaths() {
        val bytes = archive()
        val restored = BackupArchive.read(bytes.inputStream())
        assertEquals(document, restored)
        val paths = entries(bytes).keys
        assertTrue(paths.containsAll(listOf("index.json", "details.json", "songs.json", "history.json", "settings.json", "lyrics.json")))
        assertEquals(1, paths.count { it.startsWith("lyrics/") })
        assertFalse(paths.any { it.contains("unsafe") })
    }
    @Test fun archiveDetailsCarryInjectedCreationInfo() {
        val bytes = ByteArrayOutputStream().also {
            BackupArchive.write(document, it, createdAtEpochMs = 123456, appVersion = "1.2.3")
        }.toByteArray()
        val details = Json.parseToJsonElement(requireNotNull(entries(bytes)["details.json"]).toString(Charsets.UTF_8)).jsonObject
        assertEquals("123456", details.getValue("createdAtEpochMs").jsonPrimitive.content)
        assertEquals("1.2.3", details.getValue("appVersion").jsonPrimitive.content)
        assertEquals(document, BackupArchive.read(bytes.inputStream()))
        assertThrows(IllegalArgumentException::class.java) {
            BackupArchive.write(document, ByteArrayOutputStream(), createdAtEpochMs = -1)
        }
    }
    @Test fun linksOnlyPreservesProvenanceWithoutLyricContent() {
        val bytes = archive(BackupOptions(lyrics = LyricsBackupMode.LINKS_METADATA))
        val restored = BackupArchive.read(bytes.inputStream()).songs.single()
        assertTrue(restored.versions.isEmpty())
        assertEquals("https://example.com/song", restored.lyricReferences.single().sourceUrl)
        assertEquals("Credit", restored.lyricReferences.single().attribution)
        assertNull(restored.selected)
        assertFalse(entries(bytes).keys.any { it.startsWith("lyrics/") })
        assertFalse(entries(bytes).values.any { it.toString(Charsets.UTF_8).contains("Original words") })
    }
    @Test fun writingKeepsCallerStreamOpenAndUsesBulkWrites() {
        var closed = false
        var singleByteWrites = 0
        val output = object : ByteArrayOutputStream() {
            override fun write(value: Int) { singleByteWrites++; super.write(value) }
            override fun close() { closed = true; super.close() }
        }
        BackupArchive.write(document, output)
        assertFalse(closed)
        // ZIP headers can use single bytes, but compressed contents must use bulk I/O.
        assertTrue(singleByteWrites < output.size())
        assertEquals(document, BackupArchive.read(output.toByteArray().inputStream()))
    }
    @Test fun omittedSectionsContainNoSettingsHistoryOrAudioMetadata() {
        val bytes = archive(BackupOptions(lyrics = LyricsBackupMode.FULL, songMetadata = false, history = false, settings = false))
        val restored = BackupArchive.read(bytes.inputStream())
        assertTrue(restored.preferences.isEmpty())
        val song = restored.songs.single()
        assertEquals(0L, song.plays); assertNull(song.lastPlayed); assertFalse(song.favorite)
        assertNull(song.recording.album); assertNull(song.recording.durationMs); assertNull(song.recording.fileName)
        assertEquals(version, song.versions.single())
        assertFalse(entries(bytes).keys.any { it in setOf("songs.json", "history.json", "settings.json") })
    }
    @Test fun settingsOnlyAndNoLyricsAreIndependent() {
        val restored = BackupArchive.read(archive(BackupOptions(LyricsBackupMode.NONE, false, false, true)).inputStream())
        assertTrue(restored.songs.isEmpty()); assertEquals(document.preferences, restored.preferences)
        val noLyrics = BackupArchive.read(archive(BackupOptions(lyrics = LyricsBackupMode.NONE)).inputStream())
        assertTrue(noLyrics.songs.single().versions.isEmpty()); assertTrue(noLyrics.songs.single().lyricReferences.isEmpty())
        assertEquals(4L, noLyrics.songs.single().plays)
    }
    @Test fun legacyJsonRemainsReadable() {
        assertEquals(document, BackupArchive.read(BackupCodec.encode(document).byteInputStream()))
    }
    @Test fun missingUnexpectedAndUnsafeEntriesAreRejected() {
        val original = entries(archive())
        listOf(original - "history.json", original + ("unexpected.json" to "{}".toByteArray()),
            original + ("../escape.json" to "{}".toByteArray()), original - original.keys.first { it.startsWith("lyrics/") })
            .forEach { invalid -> assertThrows(IllegalArgumentException::class.java) { BackupArchive.read(zip(invalid).inputStream()) } }
    }
    @Test fun corruptedSectionIsRejectedBeforeRestore() {
        val original = entries(archive())
        assertThrows(IllegalArgumentException::class.java) {
            BackupArchive.read(zip(original + ("history.json" to "[]".toByteArray())).inputStream())
        }
    }
    @Test fun expandedEntryLimitIsEnforced() {
        val malicious = zip(mapOf("index.json" to ByteArray(BackupArchive.MAX_ENTRY_BYTES + 1)))
        assertThrows(IllegalArgumentException::class.java) { BackupArchive.read(malicious.inputStream()) }
    }
    @Test fun duplicatePathsAndTruncatedDirectoryAreRejected() {
        val valid = archive()
        assertThrows(IllegalArgumentException::class.java) { BackupArchive.read(valid.copyOf(valid.size - 1).inputStream()) }
        val duplicate = zip(mapOf("first.json" to byteArrayOf(), "other.json" to byteArrayOf()))
        val from = "other.json".toByteArray()
        val to = "first.json".toByteArray()
        for (at in 0..duplicate.size - from.size) {
            if (from.indices.all { duplicate[at + it] == from[it] }) to.copyInto(duplicate, at)
        }
        assertThrows(IllegalArgumentException::class.java) { BackupArchive.read(duplicate.inputStream()) }
    }
    @Test fun totalExpandedLimitRejectsManyIndividuallyAllowedEntries() {
        val payload = ByteArray(BackupArchive.MAX_ENTRY_BYTES)
        val bytes = ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip -> repeat(7) {
                zip.putNextEntry(ZipEntry("$it.json")); zip.write(payload); zip.closeEntry()
            } }
        }.toByteArray()
        assertThrows(IllegalArgumentException::class.java) { BackupArchive.read(bytes.inputStream()) }
    }
    @Test fun allNonemptySectionCombinationsRoundTripWithoutReintroducingOmissions() {
        LyricsBackupMode.entries.forEach { mode ->
            listOf(false, true).forEach { metadata -> listOf(false, true).forEach { history -> listOf(false, true).forEach { settings ->
                val options = BackupOptions(mode, metadata, history, settings)
                if (options.hasContent) {
                    val restored = BackupArchive.read(archive(options).inputStream())
                    assertEquals(options, restored.options)
                    assertEquals(if (settings) document.preferences else emptyMap<String, String>(), restored.preferences)
                    restored.songs.forEach { song ->
                        assertEquals(if (history) 4L else 0L, song.plays)
                        assertEquals(metadata, song.favorite)
                        assertEquals(if (metadata) "song.mp3" else null, song.recording.fileName)
                        assertEquals(if (mode == LyricsBackupMode.FULL) 1 else 0, song.versions.size)
                        assertEquals(if (mode == LyricsBackupMode.LINKS_METADATA) 1 else 0, song.lyricReferences.size)
                    }
                }
            } } }
        }
    }
}
