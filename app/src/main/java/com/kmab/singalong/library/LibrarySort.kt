package com.kmab.singalong.library

import com.kmab.singalong.model.LibraryEntry

enum class LibrarySort(val label: String) {
    RECENT("Recently played"), TITLE("Title · A–Z"), ARTIST("Artist · A–Z"), ALBUM("Album · A–Z"), MOST_PLAYED("Most played");

    fun sort(entries: List<LibraryEntry>): List<LibraryEntry> {
        val title = compareBy<LibraryEntry> { RecordingIdentity.normalize(it.recording.title) }
            .thenBy { RecordingIdentity.artists(it.recording) }.thenBy { it.recording.id }
        val order = when (this) {
            RECENT -> compareByDescending<LibraryEntry> { it.lastPlayedAtMs ?: Long.MIN_VALUE }.then(title)
            TITLE -> title
            ARTIST -> compareBy<LibraryEntry> { it.recording.artists.isEmpty() }
                .thenBy { RecordingIdentity.artists(it.recording) }.then(title)
            ALBUM -> compareBy<LibraryEntry> { it.recording.album.isNullOrBlank() }
                .thenBy { RecordingIdentity.normalize(it.recording.album.orEmpty()) }.then(title)
            MOST_PLAYED -> compareByDescending<LibraryEntry> { it.playCount }.then(title)
        }
        return entries.sortedWith(order)
    }
}
