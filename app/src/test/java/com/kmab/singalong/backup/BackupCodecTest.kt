package com.kmab.singalong.backup

import org.junit.Assert.*
import org.junit.Test

class BackupCodecTest {
    @Test fun roundTrip() { val backup = BackupDocument(songs = emptyList()); assertEquals(backup, BackupCodec.decode(BackupCodec.encode(backup))) }
    @Test(expected = IllegalArgumentException::class) fun rejectsNewerSchemas() { BackupCodec.decode("{\"schema\":2,\"songs\":[]}") }
    @Test(expected = IllegalArgumentException::class) fun boundsStreamingReads() { BackupCodec.read("oversized".byteInputStream(), 3) }
}
