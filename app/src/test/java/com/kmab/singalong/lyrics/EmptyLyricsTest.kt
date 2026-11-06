package com.kmab.singalong.lyrics

import com.kmab.singalong.model.Recording
import org.junit.Assert.*
import org.junit.Test

class EmptyLyricsTest {
    private val recording = Recording("r", "Song", listOf("Artist"), null, null)
    private val blankTiming = "[00:00.00]\n[00:12.00]   "

    @Test fun timestampOnlyResponsesAreNotSavedAsLyrics() {
        val song = ProviderSong(1, "Song", "Artist", syncedLyrics = blankTiming)
        assertTrue(LyricsMatcher.versions(recording, listOf(song)).isEmpty())
    }

    @Test fun emptyTimedResponseFallsBackToRealPlainText() {
        val parsed = LrcParser.parse(blankTiming, "Original line\n\nAnother line")
        assertEquals(listOf("Original line", "", "Another line"), parsed.map { it.text })
        assertTrue(parsed.all { it.startMs == null })
    }

    @Test fun blankLyricsfileFallsBackToLegacyLyrics() {
        val document = """
            version: "1.0"
            metadata: {title: Song, artist: Artist}
            lines:
              - {start_ms: 0, text: ""}
              - {start_ms: 1000, text: "   "}
        """.trimIndent()
        val song = ProviderSong(1, "Song", "Artist", plainLyrics = "Original line", lyricsfile = document)
        assertEquals("Original line", LyricsMatcher.versions(recording, listOf(song)).single().lines.single().text)
    }

    @Test fun instrumentalGapsWithinRealLyricsRemainIntact() {
        val lines = LrcParser.parse("$blankTiming\n[00:15.00]Original line", null)
        assertEquals(3, lines.size)
        assertEquals(listOf(0L, 12_000L, 15_000L), lines.map { it.startMs })
        assertEquals("", lines.first().text)
    }
}
