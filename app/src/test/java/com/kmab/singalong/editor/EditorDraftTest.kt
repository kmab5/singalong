package com.kmab.singalong.editor

import com.kmab.singalong.ui.EditorDraft
import org.junit.Assert.*
import org.junit.Test

class EditorDraftTest {
    @Test fun textChangesCannotShiftExistingTimingAndExplicitUntimedCopyUnlocksStructure() {
        val source = LyricsEditor.import("r", "[00:01]First\n[00:02]Second")
        val draft = EditorDraft()
        draft.load(source)
        draft.changeText("First\nInserted\nSecond")
        assertEquals(listOf(1000L, 2000L), draft.timing.value?.times)
        assertNotNull(draft.timing.value?.problem(draft.text.value.lines().size))
        draft.clearTiming()
        assertEquals(listOf(null, null, null), draft.timing.value?.times)
        assertNull(draft.timing.value?.problem(3))
        assertEquals(listOf(1000L, 2000L), source.lines.map { it.startMs })
        draft.changeText("More\nFirst\nInserted\nSecond")
        assertEquals(4, draft.timing.value?.times?.size)
        assertTrue(draft.dirty.value)
    }
    @Test fun replacingOrClosingDraftDoesNotLeakTimesIntoAnotherVersion() {
        val draft = EditorDraft()
        draft.load(LyricsEditor.import("one", "[00:01]First"))
        draft.timing.value = draft.timing.value?.copy(timeInput = "00:02.500")
        draft.load(LyricsEditor.import("two", "Untimed\nWords"))
        assertEquals(listOf(null, null), draft.timing.value?.times)
        assertEquals("", draft.timing.value?.input)
        draft.clear()
        assertNull(draft.timing.value)
        assertEquals("", draft.text.value)
        assertFalse(draft.dirty.value)
    }
}
