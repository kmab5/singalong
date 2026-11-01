package com.kmab.singalong.library

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.kmab.singalong.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class LibraryRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, LibraryDatabase::class.java).build()
    private val repository = LibraryRepository(db)
    @After fun closeDatabase() { db.close() }

    @Test fun qualifiedOccurrencesAreIdempotent() = runTest {
        val song = repository.resolveRecording(Recording("raw", "Song", listOf("Artist"), null, 120_000))
        repository.recordQualifiedPlay(song.id, "one", 10)
        repository.recordQualifiedPlay(song.id, "one", 10)
        repository.recordQualifiedPlay(song.id, "two", 20)
        val entry = repository.entries.first().single()
        assertEquals(2L, entry.playCount)
        assertEquals(20L, entry.lastPlayedAtMs)
    }

    @Test fun enrichmentKeepsIdentityAndDifferentRecordingsRemainSeparate() = runTest {
        val first = repository.resolveRecording(Recording("raw", "Song", listOf("Artist"), null, null))
        val enriched = repository.resolveRecording(first.copy(album = "Album", durationMs = 120_000), first.id)
        assertEquals(first.id, enriched.id)
        val live = repository.resolveRecording(first.copy(title = "Song (Live)", durationMs = 180_000))
        assertNotEquals(first.id, live.id)
    }

    @Test fun newProviderResultNeverOverridesSelectedPersonalVersion() = runTest {
        val song = repository.resolveRecording(Recording("raw", "Song", listOf("Artist"), null, null))
        val personal = LyricVersion("personal", song.id, "hash1", LyricOrigin.PERSONAL, listOf(LyricLine("line", null, "My own words")), null, null, MatchConfidence.CONFIDENT)
        repository.saveCandidates(song, listOf(personal))
        repository.saveCandidates(song, listOf(personal.copy(id = "provider", origin = LyricOrigin.PROVIDER, contentHash = "hash2")))
        assertEquals("personal", repository.selectedVersion(song.id)?.id)
    }
}
