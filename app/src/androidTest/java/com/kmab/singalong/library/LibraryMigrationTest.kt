package com.kmab.singalong.library

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LibraryMigrationTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), LibraryDatabase::class.java,
        emptyList(), FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun filenameMigrationPreservesSavedLyricsAndSelections() {
        helper.createDatabase("filename-migration", 2).apply {
            execSQL("INSERT INTO recordings (id, title, normalizedTitle, artistsJson, playCount, lookupStatus, favorite) VALUES ('song', 'Song', 'song', '[\"Artist\"]', 4, 'available', 1)")
            execSQL("INSERT INTO lyrics (id, recordingId, contentJson) VALUES ('lyrics', 'song', 'saved content')")
            execSQL("INSERT INTO selections (recordingId, versionId) VALUES ('song', 'lyrics')")
            close()
        }
        helper.runMigrationsAndValidate("filename-migration", 3, true, LibraryDatabase.FILE_NAME_MIGRATION).apply {
            query("SELECT r.fileName, r.playCount, r.favorite, l.contentJson FROM recordings r JOIN selections s ON s.recordingId = r.id JOIN lyrics l ON l.id = s.versionId").use {
                assertTrue(it.moveToFirst())
                assertTrue(it.isNull(0))
                assertEquals(4, it.getInt(1))
                assertEquals(1, it.getInt(2))
                assertEquals("saved content", it.getString(3))
            }
            close()
        }
    }
    @Test fun referencesMigrationPreservesExistingLyricsAndAddsIndependentProvenance() {
        helper.createDatabase("references-migration", 3).apply {
            execSQL("INSERT INTO recordings (id, title, normalizedTitle, artistsJson, playCount, lookupStatus, favorite) VALUES ('song', 'Song', 'song', '[]', 2, 'available', 0)")
            execSQL("INSERT INTO lyrics (id, recordingId, contentJson) VALUES ('lyrics', 'song', 'original')")
            execSQL("INSERT INTO selections (recordingId, versionId) VALUES ('song', 'lyrics')")
            close()
        }
        helper.runMigrationsAndValidate("references-migration", 4, true, LibraryDatabase.LYRIC_REFERENCES_MIGRATION).apply {
            execSQL("INSERT INTO lyric_references (id, recordingId, contentJson) VALUES ('reference', 'song', 'provenance')")
            query("SELECT l.contentJson, r.playCount FROM lyrics l JOIN recordings r ON r.id = l.recordingId JOIN selections s ON s.versionId = l.id").use {
                assertTrue(it.moveToFirst()); assertEquals("original", it.getString(0)); assertEquals(2, it.getInt(1))
            }
            query("SELECT contentJson FROM lyric_references").use { assertTrue(it.moveToFirst()); assertEquals("provenance", it.getString(0)) }
            close()
        }
    }
}
