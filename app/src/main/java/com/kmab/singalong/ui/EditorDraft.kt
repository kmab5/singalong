package com.kmab.singalong.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.kmab.singalong.editor.TimingDraft
import com.kmab.singalong.model.LyricVersion

/** Large drafts stay out of saved-instance-state/Binder; rotation retains text and timing together. */
class EditorDraft : ViewModel() {
    val text = mutableStateOf("")
    val source = mutableStateOf<LyricVersion?>(null)
    val dirty = mutableStateOf(false)
    val timing = mutableStateOf<TimingDraft?>(null)

    fun load(version: LyricVersion?) {
        source.value = version
        text.value = version?.lines?.joinToString("\n") { it.text }.orEmpty()
        timing.value = version?.lines?.takeIf { it.isNotEmpty() }?.let { TimingDraft(it.map { line -> line.startMs }) }
        dirty.value = false
    }
    fun changeText(value: String) {
        text.value = value
        val current = timing.value
        // Resizing an entirely untimed draft is safe. Never shift existing times onto different lines.
        if (current != null && current.times.all { it == null } && current.timeInput.isNullOrBlank()) {
            val count = value.lines().size
            if (count != current.times.size) timing.value = TimingDraft(List(count) { null })
        }
        dirty.value = true
    }
    fun clearTiming() { timing.value = TimingDraft(List(text.value.lines().size) { null }); dirty.value = true }
    fun clear() = load(null)
}
