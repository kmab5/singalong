package com.kmab.singalong.library

import com.kmab.singalong.model.Recording
import org.junit.Assert.*
import org.junit.Test

class FolderIdentityTest {
    private val tagged = Recording("tag", "Song", listOf("Artist"), "Album", 120_000)
    @Test fun filenameHintMatchesSavedPartialTagsToTaggedCopies() {
        val partial = tagged.copy(artists = emptyList(), fileName = "Artist - Song.flac")
        assertEquals(partial, FolderIdentity.match(listOf(partial), tagged.copy(id = "copy")))
        assertFalse(FolderIdentity.same(partial, tagged.copy(artists = listOf("Other"))))
    }
    @Test fun filenameAndTaggedCopyMatchWithoutRewritingOriginalMetadata() {
        val file = Recording("file", "01 - Artist - Song.mp3", emptyList(), null, 121_000)
        assertEquals(tagged, FolderIdentity.match(listOf(tagged), file))
        assertTrue(file.artists.isEmpty())
        assertEquals("01 - Artist - Song.mp3", file.title)
    }
    @Test fun distinctArtistEditionAndDurationRemainSeparate() {
        assertFalse(FolderIdentity.same(tagged, tagged.copy(artists = listOf("Other"))))
        assertFalse(FolderIdentity.same(tagged, tagged.copy(album = "Live")))
        assertFalse(FolderIdentity.same(tagged, tagged.copy(durationMs = 180_000)))
        assertFalse(FolderIdentity.same(tagged, tagged.copy(title = "Song (Live)")))
    }
    @Test fun ambiguousMissingAlbumDoesNotChooseAnArbitraryRecording() {
        val other = tagged.copy(id = "second", album = "Other album")
        assertNull(FolderIdentity.match(listOf(tagged, other), tagged.copy(album = null)))
        assertEquals(tagged, FolderIdentity.match(listOf(other, tagged), tagged.copy(id = "file")))
    }
}
