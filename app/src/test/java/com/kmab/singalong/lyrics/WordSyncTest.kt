package com.kmab.singalong.lyrics

import com.kmab.singalong.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class WordSyncTest {
    companion object {
        val yaml = """
            version: '1.0'
            metadata: {title: Synthetic, artist: Tester}
            lines:
              - text: 'Hello world'
                start_ms: 1000
                end_ms: 6000
                words:
                  - {text: 'Hello ', start_ms: 1000, end_ms: 1500}
                  - {text: world, start_ms: 4000, end_ms: 6000}
        """.trimIndent()
        val recording = Recording("r", "Synthetic", listOf("Tester"), "Album", 10000)
        private val json = Json { ignoreUnknownKeys = true }
        fun song(document: String = yaml): ProviderSong = json.decodeFromString(
            buildJsonObject {
                put("id", 1); put("trackName", "Synthetic"); put("artistName", "Tester")
                put("syncedLyrics", "[00:01]Hello world"); put("lyricsfile", document); put("hasWordSync", true)
            }.toString())
    }

    @Test fun providerWordTimingSurvivesMatchingAndSerialization() {
        val version = LyricsMatcher.versions(recording, listOf(song())).single()
        val line = Json.parseToJsonElement(Json.encodeToString(version)).jsonObject.getValue("lines").jsonArray.single().jsonObject
        assertNotNull("Provider word timestamps must be retained", line["words"])
        assertEquals(4000, line.getValue("words").jsonArray[1].jsonObject.getValue("startMs").jsonPrimitive.int)
        assertEquals(6000, line.getValue("endMs").jsonPrimitive.int)
    }

    @Test fun actualDurationsAndGapsReplaceEvenlyDividedProgress() {
        val words = LyricsfileParser.parse(yaml)!!.single().words
        assertEquals(.5f, WordSync.Timeline(words, 6000).emphasis(0, 1250, 0), .001f)
        assertEquals(1f, WordSync.Timeline(words, 6000).emphasis(0, 2500, 0), .001f)
        assertEquals(0f, WordSync.Timeline(words, 6000).emphasis(1, 2500, 0), .001f)
        assertEquals(.5f, WordSync.Timeline(words, 6000).emphasis(1, 5000, 0), .001f)
        assertEquals(0f, WordSync.Timeline(words, 6000).emphasis(1, 1100, 0), .001f)
        assertEquals(.5f, WordSync.Timeline(words, 6000).emphasis(1, 5500, 500), .001f)
        assertEquals(0f, WordSync.Timeline(words, 6000).emphasis(0, null, 0), .001f)
    }

    @Test fun overlappingWordsAndMissingEndsAdvanceIndependently() {
        val words = listOf(LyricWord("one ", 1000, 3000), LyricWord("two ", 2000), LyricWord("three", 4000))
        assertEquals(.75f, WordSync.Timeline(words, 6000).emphasis(0, 2500, 0), .001f)
        assertEquals(.25f, WordSync.Timeline(words, 6000).emphasis(1, 2500, 0), .001f)
        assertEquals(.5f, WordSync.Timeline(words, 6000).emphasis(2, 5000, 0), .001f)
        assertEquals(1f, WordSync.Timeline(words, null).emphasis(2, 4000, 0), .001f)
        assertNull(words[1].endMs)
    }

    @Test fun playbackPauseSeekSpeedAndDelayUseTheSameClock() {
        val words = LyricsfileParser.parse(yaml)!!.single().words
        val playing = PlaybackSnapshot("s", "p", "p", recording, PlaybackStatus.PLAYING, 4000, 100, 2f)
        assertEquals(.5f, WordSync.Timeline(words, 6000).emphasis(1, PlaybackClock.position(playing, 600), 0), .001f)
        val paused = playing.copy(status = PlaybackStatus.PAUSED, positionMs = 5000)
        assertEquals(.5f, WordSync.Timeline(words, 6000).emphasis(1, PlaybackClock.position(paused, 10_000), 0), .001f)
        val seek = paused.copy(positionMs = 1250)
        assertEquals(0f, WordSync.Timeline(words, 6000).emphasis(1, PlaybackClock.position(seek, 10_000), 0), .001f)
    }

    @Test fun timingOnlyChangesHaveDistinctIdentitiesAndEqualMatchesPreferWords() {
        val timed = LyricsMatcher.versions(recording, listOf(song())).single()
        val changed = LyricsMatcher.versions(recording, listOf(song(yaml.replace("start_ms: 4000", "start_ms: 4500")))).single()
        val lineOnly = LyricsMatcher.versions(recording, listOf(song().copy(lyricsfile = null))).single()
        assertNotEquals(timed.contentHash, changed.contentHash)
        assertNotEquals(timed.contentHash, lineOnly.contentHash)
        assertEquals(timed, LyricPreference.sorted(listOf(lineOnly, timed), recording.durationMs).first())
        assertEquals("Word-synced", WordSync.label(timed))
        assertEquals("Line-synced", WordSync.label(lineOnly))
    }

    @Test fun lineOnlyLyricsfileKeepsTheExistingLrcIdentity() {
        val document = """
            version: '1.0'
            metadata: {title: Synthetic, artist: Tester}
            lines: [{text: 'Hello world', start_ms: 1000}]
        """.trimIndent()
        val legacy = LyricsMatcher.versions(recording, listOf(song().copy(lyricsfile = null))).single()
        val structured = LyricsMatcher.versions(recording, listOf(song(document))).single()
        assertEquals(legacy.id, structured.id)
        assertEquals(legacy.contentHash, structured.contentHash)
        assertEquals(legacy.lines, structured.lines)
    }
}
