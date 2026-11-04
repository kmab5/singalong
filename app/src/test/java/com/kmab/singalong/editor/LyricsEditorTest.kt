package com.kmab.singalong.editor

import org.junit.Assert.*
import org.junit.Test

class LyricsEditorTest {
    private val original = LyricsEditor.import("r", "\uFEFF[00:01]世界\n[00:02]Synthetic words")
    @Test fun timedEditsPreserveIdentityOfLinesButCreateANewVersion() {
        val edited = LyricsEditor.edit(original, "New words\nOther words", true)
        assertEquals(original.lines.map { it.startMs }, edited.lines.map { it.startMs })
        assertEquals(original.lines.map { it.id }, edited.lines.map { it.id })
        assertNotEquals(original.id, edited.id)
        assertNotEquals(original.contentHash, edited.contentHash)
        assertEquals("世界", original.lines.first().text)
    }
    @Test(expected = IllegalArgumentException::class) fun timedEditsCannotAddLines() { LyricsEditor.edit(original, "a\nb\nc", true) }
    @Test fun untimedCopiesCanChangeStructure() { assertTrue(LyricsEditor.edit(original, "a\nb\nc", false).lines.all { it.startMs == null }) }
    @Test(expected = IllegalArgumentException::class) fun emptyImportRejected() { LyricsEditor.import("r", " ") }

    @Test fun onlyChangedTextOrTimingDropsWordSyncAndOriginalRemainsSaved() {
        val lines = com.kmab.singalong.lyrics.LyricsfileParser.parse(com.kmab.singalong.lyrics.WordSyncTest.yaml)!!
        val source = original.copy(lines = lines + lines.single().copy(id = "second", startMs = 7000, words = emptyList(), endMs = 8000))
        val unchanged = LyricsEditor.edit(source, "Hello world\nHello world", true)
        assertEquals(source.lines, unchanged.lines)
        val edited = LyricsEditor.edit(source, "Hello earth\nHello world", true)
        assertTrue(edited.lines.first().words.isEmpty())
        assertEquals(source.lines[1], edited.lines[1])
        val retimed = LyricsEditor.editTiming(source, "Hello world\nHello world", listOf(2000, 7000))
        assertTrue(retimed.lines.first().words.isEmpty()); assertNull(retimed.lines.first().endMs)
        val timingUnchanged = LyricsEditor.editTiming(source, "Hello world\nHello world", listOf(1000, 7000))
        assertEquals(source.lines, timingUnchanged.lines)
        val untimed = LyricsEditor.edit(source, "Hello world\nHello world", false)
        assertTrue(untimed.lines.all { it.words.isEmpty() && it.startMs == null && it.endMs == null })
        assertEquals(2, source.lines.first().words.size)
    }

    @Test fun unchangedMultilineSegmentsKeepWordsAndEditingRequiresAnUntimedCopy() {
        val line = com.kmab.singalong.model.LyricLine("multi", 1000, "one\ntwo", 3000,
            listOf(com.kmab.singalong.model.LyricWord("one\n", 1000, 2000), com.kmab.singalong.model.LyricWord("two", 2000, 3000)))
        val source = original.copy(lines = listOf(line))
        assertEquals(source.lines, LyricsEditor.edit(source, "one\ntwo", true).lines)
        assertThrows(IllegalArgumentException::class.java) { LyricsEditor.edit(source, "one\nchanged", true) }
        val converted = LyricsEditor.edit(source, "one\nchanged", false)
        assertEquals(listOf("one", "changed"), converted.lines.map { it.text })
        assertTrue(converted.lines.all { it.startMs == null && it.words.isEmpty() })
        assertEquals(2, source.lines.single().words.size)
    }
}
