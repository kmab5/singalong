package com.kmab.singalong.backup

import org.junit.Assert.*
import org.junit.Test

class BackupCodecTest {
    @Test fun roundTrip() { val backup = BackupDocument(songs = emptyList()); assertEquals(backup, BackupCodec.decode(BackupCodec.encode(backup))) }
    @Test(expected = IllegalArgumentException::class) fun rejectsNewerSchemas() { BackupCodec.decode("{\"schema\":2,\"songs\":[]}") }
    @Test(expected = IllegalArgumentException::class) fun boundsStreamingReads() { BackupCodec.read("oversized".byteInputStream(), 3) }

    private fun wordBackup(): BackupDocument {
        val r = com.kmab.singalong.lyrics.WordSyncTest.recording
        val v = com.kmab.singalong.lyrics.LyricsMatcher.versions(r, listOf(com.kmab.singalong.lyrics.WordSyncTest.song())).single()
        return BackupDocument(songs = listOf(BackupSong(r, 0, null, false, listOf(v), v.id)))
    }
    @Test fun wordsAndEndsRoundTripAndLegacyLinesHaveNoInventedWords() {
        val backup = wordBackup()
        assertEquals(backup, BackupCodec.decode(BackupCodec.encode(backup)))
        val line = kotlinx.serialization.json.Json.decodeFromString<com.kmab.singalong.model.LyricLine>("""{"id":"x","startMs":1000,"text":"old words"}""")
        assertTrue(line.words.isEmpty()); assertNull(line.endMs)
    }
    @Test fun rejectsInvalidWordAlignmentAndReversedEndTimes() {
        val backup = wordBackup()
        val song = backup.songs.single()
        val version = song.versions.single()
        val line = version.lines.single()
        val invalid = listOf(line.copy(text = "different"), line.copy(endMs = 1),
            line.copy(words = line.words.map { it.copy(startMs = -1) }),
            line.copy(words = line.words.map { it.copy(endMs = it.startMs - 1) }))
        invalid.forEach { bad ->
            assertThrows(IllegalArgumentException::class.java) {
                BackupCodec.encode(backup.copy(songs = listOf(song.copy(versions = listOf(version.copy(lines = listOf(bad)))))))
            }
        }
    }
}
