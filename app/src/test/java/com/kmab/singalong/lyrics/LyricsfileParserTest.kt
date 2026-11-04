package com.kmab.singalong.lyrics

import org.junit.Assert.*
import org.junit.Test

class LyricsfileParserTest {
    @Test fun preservesSegmentsSpacesAndAbsoluteTimes() {
        val line = LyricsfileParser.parse(WordSyncTest.yaml)!!.single()
        assertEquals("Hello world", line.words.joinToString("") { it.text })
        assertEquals(listOf(1000L, 4000L), line.words.map { it.startMs })
        assertEquals(listOf(1500L, 6000L), line.words.map { it.endMs })
        assertEquals(line.text.length, WordSync.ranges(line.words).last().end)
    }

    @Test fun unicodeAndWhitespaceArePreservedWithoutSplittingOnSpaces() {
        val yaml = WordSyncTest.yaml.replace("Hello world", "世界🎶").replace("Hello ", "世界").replace("text: world", "text: 🎶")
        val line = LyricsfileParser.parse(yaml)!!.single()
        assertEquals(listOf("世界", "🎶"), WordSync.ranges(line.words).map { line.text.substring(it.start, it.end) })
        assertEquals(listOf(0, 2), WordSync.ranges(line.words).map { it.start })
    }

    @Test fun malformedWordDataRetainsLineTextAndTimestamp() {
        for (yaml in listOf(
            WordSyncTest.yaml.replace("text: world", "text: different"),
            WordSyncTest.yaml.replace("end_ms: 1500", "end_ms: 500"),
            WordSyncTest.yaml.replace("start_ms: 4000", "start_ms: -1"),
            WordSyncTest.yaml.replace("start_ms: 4000", "start_ms: 4.5"),
        )) {
            val line = LyricsfileParser.parse(yaml)!!.single()
            assertEquals("Hello world", line.text); assertEquals(1000L, line.startMs)
            assertTrue(line.words.isEmpty())
        }
    }

    @Test fun unsupportedOrUnsafeDocumentsFallBackToLrc() {
        for (yaml in listOf(
            WordSyncTest.yaml.replace("'1.0'", "'2.0'"),
            WordSyncTest.yaml.replace("version: '1.0'", "version: '1.0'\nversion: '1.0'"),
            WordSyncTest.yaml.replace("artist: Tester", "artist: Tester, offset_ms: 100"),
            WordSyncTest.yaml.replace("start_ms: 1000", "start_ms: 99999999999999999999999999999"),
            "!!java.net.URL ['https://example.invalid']",
            "a: &a [x]\nb: *a",
            "[".repeat(100) + "x" + "]".repeat(100),
            "x".repeat(2_000_001),
        )) {
            assertNull(LyricsfileParser.parse(yaml))
            val fallback = LyricsMatcher.versions(WordSyncTest.recording, listOf(WordSyncTest.song(yaml))).single()
            assertEquals("Hello world", fallback.lines.single().text)
            assertTrue(fallback.lines.single().words.isEmpty())
        }
    }

    @Test fun supportsMultilineScalarsOptionalEndsAndOverlappingWords() {
        val yaml = """
            version: '1.0'
            metadata: {title: Test, artist: Test}
            lines:
              - text: |-
                  one
                  two
                start_ms: 1000
                words:
                  - {text: "one\n", start_ms: 1000, end_ms: 3000}
                  - {text: two, start_ms: 2000}
        """.trimIndent()
        val line = LyricsfileParser.parse(yaml)!!.single()
        assertEquals("one\ntwo", line.text)
        assertEquals(2, line.words.size)
        assertNull(line.endMs); assertNull(line.words.last().endMs)
    }
}
