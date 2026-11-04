package com.kmab.singalong.lyrics

import com.kmab.singalong.library.RecordingIdentity.normalize
import com.kmab.singalong.model.Recording

data class SearchQuery(val recording: Recording, val inferred: Boolean)

/** Search hints only: never write guessed fields back into a recording. */
object SearchQueries {
    private val extension = Regex("\\.(mp3|flac|m4a|aac|ogg|opus|wav|wma|aiff|alac|mp4|webm)$", RegexOption.IGNORE_CASE)
    private val trackPrefix = Regex("^(?:\\d{1,3}\\s*[-.]\\s*|0\\d{1,2}\\s+)(?=\\S)")
    private val separator = Regex("\\s+[-–—|]\\s+")
    private val decoration = Regex("[\\[(](?:official(?: music)? (?:video|audio)|official|lyrics?|lyric video|audio|video|hd|hq|4k)[\\])]", RegexOption.IGNORE_CASE)
    private val unknown = setOf("", "unknown", "<unknown>", "[unknown]", "unknown artist", "unknown album")
    private fun known(value: String?) = normalize(value.orEmpty()) !in unknown
    private fun spaces(value: String) = value.replace(Regex("\\s+"), " ").trim()
    private fun same(a: String, b: String) = normalize(a) == normalize(b)

    fun plan(original: Recording): List<SearchQuery> {
        val file = extension.containsMatchIn(original.title.trim())
        val rawTitle = if (file) original.title.trim().substringAfterLast('/').substringAfterLast('\\') else original.title
        val cleanTitle = spaces(decoration.replace(extension.replace(rawTitle, "").replace('_', ' '), ""))
        val title = trackPrefix.replace(cleanTitle, "")
        val artists = original.artists.filter(::known).map { spaces(it.replace('_', ' ')) }
        val knownArtist = artists.joinToString(", ")
        val parts = separator.split(title)
        var inferredTitle = title
        var inferredArtists = artists
        var inferredAlbum = original.album?.takeIf(::known)

        if (parts.size == 4 && parts[2].matches(Regex("\\d{1,3}")) &&
            (artists.isEmpty() || same(knownArtist, parts[0]))) {
            inferredArtists = if (artists.isEmpty()) listOf(parts[0]) else artists
            inferredTitle = parts[3]
            if (inferredAlbum == null) inferredAlbum = parts[1]
        } else if (parts.size >= 2) {
            when {
                artists.isNotEmpty() && same(knownArtist, parts.first()) ->
                    inferredTitle = title.substringAfter(separator.find(title)!!.value)
                artists.isNotEmpty() && same(knownArtist, parts.last()) ->
                    inferredTitle = title.substringBeforeLast(separator.findAll(title).last().value)
                artists.isEmpty() && parts.size == 2 && known(parts[0]) -> {
                    inferredArtists = listOf(parts[0]); inferredTitle = parts[1]
                }
            }
        }
        val derived = original.copy(title = inferredTitle, artists = inferredArtists, album = inferredAlbum)
        // Try recognized hints before asking slow fallback sources about a literal filename.
        val candidates = listOf(derived, original)
        val tagged = candidates.filter { it.title.isNotBlank() && it.artists.isNotEmpty() && it.artists.all(::known) }
            .map { SearchQuery(it, it != original) }
        val fromFile = original.fileName?.takeIf { it.isNotBlank() && it != original.title }?.let { name ->
            plan(original.copy(title = name, fileName = null)).map {
                SearchQuery(it.recording.copy(fileName = name), inferred = true)
            }
        }.orEmpty()
        return (tagged + fromFile).distinctBy { Triple(it.recording.title, it.recording.artists, it.recording.album) }.take(3)
    }

    fun suggestion(original: Recording): Recording? = plan(original).firstOrNull { it.inferred }?.recording
}
