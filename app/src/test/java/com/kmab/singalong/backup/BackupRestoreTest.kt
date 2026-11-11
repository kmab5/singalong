package com.kmab.singalong.backup

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class BackupRestoreTest {
    @Test fun malformedSelectedSettingsNeverWriteLibrary() = runTest {
        var libraryWrites = 0
        try {
            restoreBackup(BackupDocument(songs = emptyList()), true,
                { throw IllegalArgumentException("Bad preference type") }, { libraryWrites++; 1 }, {})
            fail("Expected validation failure")
        } catch (_: IllegalArgumentException) { }
        assertEquals(0, libraryWrites)
    }

    @Test fun unselectedSettingsAreIgnoredAndWriteFailureReportsCommittedLibrary() = runTest {
        val document = BackupDocument(songs = emptyList())
        val ignored = restoreBackup(document, false, { fail("Must not inspect omitted settings") }, { 2 }, { fail("Must not apply settings") })
        assertEquals(BackupRestoreResult(2, SettingsRestoreStatus.NOT_SELECTED), ignored)
        val partial = restoreBackup(document, true, {}, { 3 }, { throw java.io.IOException("Full disk") })
        assertEquals(BackupRestoreResult(3, SettingsRestoreStatus.WRITE_FAILED), partial)
    }
}
