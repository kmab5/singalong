package com.kmab.singalong.library

import com.kmab.singalong.model.*
import org.junit.Assert.*
import org.junit.Test

class LibrarySortTest {
    private fun entry(id: String, title: String, artist: String, album: String?, last: Long?, plays: Long) =
        LibraryEntry(Recording(id, title, listOf(artist), album, null), last, plays, true, "available")

    @Test fun sortUsesRequestedFieldAndStableTiesWithUnknownDatesAndAlbumsLast() {
        val a = entry("a", "alpha", "Zed", null, null, 2)
        val b = entry("b", "Beta", "Amy", "First", 20, 1)
        val c = entry("c", "alpha", "Zed", "Second", 10, 2)
        val input = listOf(c, b, a)
        assertEquals(listOf(a, c, b), LibrarySort.TITLE.sort(input))
        assertEquals(listOf(b, a, c), LibrarySort.ARTIST.sort(input))
        assertEquals(listOf(b, c, a), LibrarySort.ALBUM.sort(input))
        assertEquals(listOf(b, c, a), LibrarySort.RECENT.sort(input))
        assertEquals(listOf(a, c, b), LibrarySort.MOST_PLAYED.sort(input))
    }
}
