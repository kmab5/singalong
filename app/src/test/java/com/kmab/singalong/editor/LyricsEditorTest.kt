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
}
