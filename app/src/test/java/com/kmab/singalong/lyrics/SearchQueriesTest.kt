package com.kmab.singalong.lyrics

import com.kmab.singalong.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SearchQueriesTest {
    private fun recording(title: String, artist: String = "<unknown>") = Recording("r", title, listOf(artist), null, 120_000)
    @Test fun explicitAlbumLayoutAndUnicodeAreUnderstood() {
        val query = SearchQueries.plan(recording("歌手 - 夜のアルバム - 03 - 月の歌.flac")).first()
        assertEquals("月の歌", query.recording.title)
        assertEquals(listOf("歌手"), query.recording.artists)
        assertEquals("夜のアルバム", query.recording.album)
        assertTrue(query.inferred)
    }
    @Test fun goodMetadataIsSearchedUnchangedAndAlbumIsNotGuessedFromThreeParts() {
        val clean = recording("Song (Live)", "Test Artist").copy(album = "Known Album")
        assertEquals(listOf(SearchQuery(clean, false)), SearchQueries.plan(clean))
        assertTrue(SearchQueries.plan(recording("Artist - Album - Title.mp3")).isEmpty())
        assertTrue(SearchQueries.plan(recording("Track03.mp3")).isEmpty())
    }
    @Test fun knownArtistDisambiguatesTitleFirstAndHyphensInsideNamesSurvive() {
        val query = SearchQueries.plan(recording("03. My_Song - AC-DC.mp3", "AC-DC")).first().recording
        assertEquals("My Song", query.title)
        assertEquals(listOf("AC-DC"), query.artists)
        assertNull(query.album)
        assertTrue(SearchQueries.plan(recording("Other Artist - My Song.mp3", "Known Artist")).all {
            it.recording.artists == listOf("Known Artist")
        })
    }
    @Test fun versionQualifiersAreNeverRemoved() {
        val title = SearchQueries.plan(recording("Artist - Song (Live Remix) [Official Video].mp3")).first().recording.title
        assertEquals("Song (Live Remix)", title)
        assertEquals("99 Problems", SearchQueries.plan(recording("99 Problems.mp3", "Artist")).first().recording.title)
        assertEquals("1979", SearchQueries.plan(recording("1979", "Artist")).single().recording.title)
        assertEquals("My Song", SearchQueries.plan(recording("Artist - My Song", "Artist")).first().recording.title)
    }
    @Test fun inferredMatchesStillRejectWrongArtistVersionAndDuration() = runTest {
        val badSongs = listOf(
            ProviderSong(1, "Song", "Other Artist", plainLyrics = "words"),
            ProviderSong(2, "Song (Live)", "Artist", plainLyrics = "words"),
            ProviderSong(3, "Song", "Artist", duration = 250.0, plainLyrics = "words"),
        )
        val search = LyricSearch(object : LyricsProvider {
            override suspend fun search(recording: Recording) = ProviderResult.Songs(badSongs)
        })
        assertEquals("not_found", search.search(recording("Artist - Song.mp3"), { true }).status)
    }
    @Test fun searchIsBoundedAndStopsWhenNetworkPolicyChanges() = runTest {
        var calls = 0
        val search = LyricSearch(object : LyricsProvider {
            override suspend fun search(recording: Recording): ProviderResult {
                calls++; return ProviderResult.Songs(emptyList())
            }
        })
        val input = recording("Song [Official Audio]", "Artist")
        assertEquals("offline", search.search(input, { calls == 0 }).status)
        assertEquals(1, calls)
        calls = 0
        assertEquals("not_found", search.search(input, { true }).status)
        assertEquals(2, calls)
    }
}
