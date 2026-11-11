package com.kmab.singalong.backup

import com.kmab.singalong.model.*
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
    @Test fun adjustableLimitAppliesToWritingAndTotalExpandedReadingIncludingIndex() {
        val valid = archive()
        val expanded = entries(valid).values.sumOf { it.size.toLong() }
        assertEquals(document, BackupArchive.read(valid.inputStream(), BackupLimits(expanded)))
        assertThrows(IllegalArgumentException::class.java) { BackupArchive.read(valid.inputStream(), BackupLimits(expanded - 1)) }
        assertThrows(IllegalArgumentException::class.java) { BackupArchive.write(document, ByteArrayOutputStream(), limits = BackupLimits(64)) }
        assertThrows(IllegalArgumentException::class.java) { BackupLimits(0) }
        assertThrows(IllegalArgumentException::class.java) { BackupLimits(-1) }
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
    @Test fun unlimitedAndLargerLimitsRoundTripValidDataBeyondFormerEntryAndTotalCaps() {
        val lines = List(8) { LyricLine("line-$it", null, "x".repeat(100_000)) }
        val versions = List(60) { version.copy(id = "large-$it", lines = lines) }
        val large = document.copy(songs = listOf(document.songs.single().copy(versions = versions, selected = "large-0")),
            preferences = mapOf("large-setting" to "s".repeat(9 * 1024 * 1024)))
        assertThrows(IllegalArgumentException::class.java) { BackupArchive.write(large, ByteArrayOutputStream()) }
        val bytes = ByteArrayOutputStream().also { BackupArchive.write(large, it, limits = BackupLimits(null)) }.toByteArray()
        assertThrows(IllegalArgumentException::class.java) { BackupArchive.read(bytes.inputStream()) }
        val restored = BackupArchive.read(bytes.inputStream(), BackupLimits(null))
        assertEquals(60, restored.songs.single().versions.size)
        assertEquals(lines, restored.songs.single().versions.last().lines)
        assertEquals(9 * 1024 * 1024, restored.preferences.getValue("large-setting").length)
        assertEquals(large, BackupArchive.read(bytes.inputStream(), BackupLimits(64L * 1024 * 1024)))
    }
    @Test fun temporaryArchiveIsRemovedAfterSuccessFailureAndInputFailure() {
        val directory = Files.createTempDirectory("backup-cleanup").toFile()
        try {
            val valid = archive()
            assertEquals(document, BackupArchive.read(valid.inputStream(), scratchDirectory = directory))
            assertTrue(directory.listFiles().orEmpty().isEmpty())
            assertThrows(IllegalArgumentException::class.java) { BackupArchive.read(valid.inputStream(), BackupLimits(64), directory) }
            assertTrue(directory.listFiles().orEmpty().isEmpty())
            val broken = object : java.io.InputStream() {
                private val prefix = intArrayOf(0x50, 0x4b, 3, 4)
                private var position = 0
                override fun read(): Int = if (position < prefix.size) prefix[position++] else throw java.io.IOException("injected read failure")
            }
            assertThrows(java.io.IOException::class.java) { BackupArchive.read(broken, scratchDirectory = directory) }
            assertTrue(directory.listFiles().orEmpty().isEmpty())
        } finally { directory.delete() }
    }
    @Test fun readsZip64Directory() {
        val original = archive()
        val end = original.size - 22
        val eocd = ByteBuffer.wrap(original, end, 22).slice().order(ByteOrder.LITTLE_ENDIAN)
        val count = eocd.getShort(10).toLong() and 0xffff
        val directorySize = eocd.getInt(12).toLong() and 0xffffffffL
        val directoryOffset = eocd.getInt(16).toLong() and 0xffffffffL
        val tail = ByteBuffer.allocate(56 + 20 + 22).order(ByteOrder.LITTLE_ENDIAN)
        tail.putInt(0x06064b50).putLong(44).putShort(45).putShort(45).putInt(0).putInt(0)
            .putLong(count).putLong(count).putLong(directorySize).putLong(directoryOffset)
        tail.putInt(0x07064b50).putInt(0).putLong(end.toLong()).putInt(1)
        tail.putInt(0x06054b50).putShort(0).putShort(0).putShort(-1).putShort(-1).putInt(-1).putInt(-1).putShort(0)
        assertEquals(document, BackupArchive.read((original.copyOf(end) + tail.array()).inputStream()))
    }
    @Test fun legacyJsonUsesSelectedLimitsAndRetainsLargeSettingsValues() {
        val settings = document.copy(preferences = (0..60).associate { "key-$it" to "x".repeat(2000) })
        val text = BackupCodec.encode(settings, BackupLimits(null))
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(settings, BackupLimits(100)) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(text, BackupLimits(100)) }
        assertThrows(IllegalArgumentException::class.java) { BackupArchive.read(text.byteInputStream(), BackupLimits(100)) }
        assertEquals(settings, BackupArchive.read(text.byteInputStream(), BackupLimits(null)))
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
