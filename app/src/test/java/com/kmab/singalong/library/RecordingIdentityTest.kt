package com.kmab.singalong.library

import com.kmab.singalong.model.Recording
import org.junit.Assert.*
import org.junit.Test

class RecordingIdentityTest {
    private val studio = Recording("studio", "A Song", listOf("Artist"), "Album", 180_000)
    @Test fun studioAndLiveVersionsStayDistinct() {
        assertFalse(RecordingIdentity.compatible(studio, studio.copy(title = "A Song (Live)")))
        assertFalse(RecordingIdentity.compatible(studio, studio.copy(durationMs = 205_000)))
    }
    @Test fun displayMetadataIsPreservedWhileComparisonIgnoresCaseAndSpacing() {
        assertTrue(RecordingIdentity.compatible(studio, studio.copy(title = " a   song ", artists = listOf("ARTIST"))))
        assertEquals("A Song", studio.title)
    }
    @Test fun missingAlbumCanBeEnrichedButDifferentArtistsCannotMerge() {
        assertTrue(RecordingIdentity.compatible(studio, studio.copy(album = null)))
        assertFalse(RecordingIdentity.compatible(studio, studio.copy(artists = listOf("Other Artist"))))
    }
}
