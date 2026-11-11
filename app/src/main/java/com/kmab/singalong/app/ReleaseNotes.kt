package com.kmab.singalong.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class ReleaseNotes(val releases: List<ReleaseNote>) {
    fun forVersion(version: String): ReleaseNote? = releases.firstOrNull { it.version == version.substringBefore("-rc.") }

    companion object {
        fun decode(text: String): ReleaseNotes = Json.decodeFromString<ReleaseNotes>(text).also { notes ->
            require(notes.releases.map { it.version }.distinct().size == notes.releases.size)
            notes.releases.forEach { release ->
                require(release.version.matches(Regex("\\d+\\.\\d+\\.\\d+")) && release.title.isNotBlank() && release.summary.isNotBlank())
                require(release.sections.isNotEmpty() && release.sections.map { it.kind }.distinct().size == release.sections.size)
                release.sections.forEach { section ->
                    require(section.kind in setOf("new", "improved", "fixed") && section.items.isNotEmpty())
                    require(section.items.all { it.title.isNotBlank() && it.body.isNotBlank() })
                }
            }
        }
    }
}
@Serializable data class ReleaseNote(val version: String, val title: String, val summary: String, val sections: List<ReleaseSection>)
@Serializable data class ReleaseSection(val kind: String, val items: List<ReleaseItem>)
@Serializable data class ReleaseItem(val title: String, val body: String)
