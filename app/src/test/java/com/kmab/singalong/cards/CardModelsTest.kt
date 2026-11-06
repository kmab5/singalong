package com.kmab.singalong.cards

import com.kmab.singalong.model.*
import org.junit.Assert.*
import org.junit.Test

class CardModelsTest {
    private val lines = listOf(LyricLine("a", 0, "First original"), LyricLine("b", 1000, ""), LyricLine("c", 2000, "آخر سطر"))
    private val content = CardContent(Recording("r", "Title", listOf("Artist"), "Album", null), lines, "LRCLIB", 0)

    @Test fun personalCopiesKeepTheirOriginalAttributionAndRejectAnotherSongsMetadata() {
        val version = LyricVersion("v", "r", "hash", LyricOrigin.PERSONAL, lines,
            "Personal edit", null, MatchConfidence.CONFIDENT, attribution = "Lyrics from LRCLIB")
        val captured = CardContent.capture(content.recording, version, 0)!!
        assertEquals("Personal edit · Lyrics from LRCLIB", captured.source)
        assertNull(CardContent.capture(content.recording.copy(id = "new-song"), version, 0))
        assertNull(CardContent.capture(content.recording, version, 1))
        assertEquals("LRCLIB", CardContent.capture(content.recording,
            version.copy(sourceName = "LRCLIB", attribution = "LRCLIB"), 0)!!.source)
    }

    @Test fun excerptKeepsOriginalOrderingUnicodeAndBlankLinesWithoutChangingSource() {
        assertEquals("First original\n\nآخر سطر", content.excerpt(setOf(2, 0, 1)))
        assertEquals("آخر سطر", content.excerpt(setOf(2, -1, 100)))
        assertEquals(lines, content.lines)
    }
    @Test fun noBlankOrOversizedExcerptIsExportable() {
        assertNotNull(content.selectionError(emptySet()))
        assertNotNull(content.selectionError(setOf(1)))
        assertNull(content.selectionError(setOf(0, 2)))
        assertNotNull(content.copy(lines = listOf(LyricLine("long", null, "x".repeat(6001)))).selectionError(setOf(0)))
    }
    @Test fun selectingATemplateRestoresItsDesignWithoutChangingTheExcerpt() {
        val original = content.excerpt(setOf(0))
        val paper = CardTemplate.PAPER.style()
        assertNotEquals(CardTemplate.GLASS.style().palette, paper.palette)
        assertEquals(original, content.excerpt(setOf(0)))
        assertTrue(CardAspect.entries.all { it.width > 0 && it.height > 0 && it.width.toLong() * it.height <= 2_100_000 })
    }
}
