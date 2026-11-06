package com.kmab.singalong.cards

import com.kmab.singalong.model.LyricLine
import com.kmab.singalong.model.Recording
import org.junit.Assert.*
import org.junit.Test

class CardCurationTest {
    private val content = CardContent(Recording("r", "Song", listOf("Artist"), "Album", null,
        fileName = "/Music/Artist/track.mp3"), listOf(LyricLine("l", null, "Original words")), "Source", 0)

    @Test fun metadataIsOptInAndNeverInvented() {
        val hidden = CardDetails(title = false, artist = false, album = false)
        assertTrue(content.metadata(hidden).isEmpty())
        assertEquals(listOf("track.mp3", "/Music/Artist"),
            content.metadata(hidden.copy(fileName = true, location = true)).map { it.second })
        assertTrue(content.copy(file = CardFileDetails()).metadata(hidden.copy(fileName = true, location = true)).isEmpty())
        assertEquals("Original words", content.excerpt(setOf(0)))
    }

    @Test fun fileLocationRejectsRemoteAndOpaqueUris() {
        assertEquals(CardFileDetails("a song.flac", "/storage/Music"), CardFileDetails.from("file:///storage/Music/a%20song.flac"))
        assertEquals(CardFileDetails("track.mp3", null), CardFileDetails.from("track.mp3"))
        assertEquals(CardFileDetails(), CardFileDetails.from("https://example.com/private/track.mp3?token=secret"))
        assertEquals(CardFileDetails(), CardFileDetails.from("content://media/external/audio/media/31"))
        assertEquals(CardFileDetails(), CardFileDetails.from("spotify:track:123"))
        assertEquals(CardFileDetails(), CardFileDetails.from("https:private-token"))
        assertEquals(CardFileDetails("song.mp3", "D:/Music"), CardFileDetails.from("D:/Music/song.mp3"))
        assertEquals(CardFileDetails(), CardFileDetails.from(null))
    }

    @Test fun galleryCoverRemainsCenteredWhenFittedToDifferentFormats() {
        for (aspect in CardAspect.entries) {
            val cover = requireNotNull(CardComposition.create(CardTemplate.GALLERY, aspect, true).cover)
            assertEquals(.5f, cover.x + cover.width / 2f, .0001f)
            assertEquals(cover.width * aspect.width, cover.height * aspect.height, .001f)
        }
    }

    @Test fun changingLookPreservesFormatAndPrivacyChoices() {
        val draft = CardStyle(aspect = CardAspect.STORY, details = CardDetails(title = false, location = true),
            showCover = false, showAppName = true)
        CardTemplate.entries.forEach { template ->
            val changed = draft.withTemplate(template)
            assertEquals(draft.aspect, changed.aspect)
            assertEquals(draft.details, changed.details)
            assertFalse(changed.showCover)
            assertTrue(changed.showAppName)
            assertEquals(template, changed.template)
        }
    }

    @Test fun compositionsReserveSeparateAreasForWordsAndArtworkInEveryFormat() {
        for (template in CardTemplate.entries) for (aspect in CardAspect.entries) {
            val layout = CardComposition.create(template, aspect, true)
            val reserved = listOfNotNull(layout.lyrics, layout.title, layout.details, layout.cover, layout.brand)
            assertTrue(reserved.all { it.x >= 0 && it.y >= 0 && it.right <= 1 && it.bottom <= 1 && it.width > 0 && it.height > 0 })
            reserved.forEachIndexed { i, a -> reserved.drop(i + 1).forEach { b -> assertFalse("$template / $aspect: $a overlaps $b", a.overlaps(b)) } }
            val withoutCover = CardComposition.create(template, aspect, false)
            assertNull(withoutCover.cover)
            assertTrue(withoutCover.lyrics.width * withoutCover.lyrics.height >= layout.lyrics.width * layout.lyrics.height)
        }
    }
}
