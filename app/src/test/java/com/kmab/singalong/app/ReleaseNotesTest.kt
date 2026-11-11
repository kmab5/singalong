package com.kmab.singalong.app

import com.kmab.singalong.BuildConfig
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class ReleaseNotesTest {
    @Test fun sharedNotesCoverThisBuildWithoutRelabellingOlderContent() {
        val notes = ReleaseNotes.decode(File(requireNotNull(System.getProperty("singalong.releaseNotes"))).readText())
        val current = requireNotNull(notes.forVersion(BuildConfig.VERSION_NAME))
        assertEquals(BuildConfig.VERSION_NAME.substringBefore("-rc."), current.version)
        assertNotNull(notes.forVersion("${current.version}-rc.1"))
        assertNull(notes.forVersion("999.0.0"))
    }

    @Test fun invalidSectionDoesNotBecomeMislabelledNews() {
        assertThrows(IllegalArgumentException::class.java) {
            ReleaseNotes.decode("""{"releases":[{"version":"1.0.0","title":"Title","summary":"Summary","sections":[{"kind":"unknown","items":[{"title":"Title","body":"Body"}]}]}]}""")
        }
    }
}
