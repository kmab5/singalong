package com.kmab.singalong.lyrics

import com.kmab.singalong.model.LyricLine
import com.kmab.singalong.model.LyricWord
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.error.YAMLException

/** Lyricsfile 1.0 is a draft. Unsupported documents fall back to the provider's legacy LRC/plain. */
object LyricsfileParser {
    private class InvalidDocument : RuntimeException()
    private fun invalid(): Nothing = throw InvalidDocument()
    private fun Any?.mapping(): Map<*, *> = this as? Map<*, *> ?: invalid()
    private fun Any?.text(): String = this as? String ?: invalid()
    private fun Any?.millis(): Long = when (this) {
        is Int -> toLong()
        is Long -> this
        else -> invalid()
    }.also { if (it < 0) invalid() }

    fun parse(document: String?): List<LyricLine>? {
        if (document.isNullOrBlank() || document.length > 2_000_000 || document.toByteArray().size > 2_000_000) return null
        return try {
            val options = LoaderOptions().apply {
                isAllowDuplicateKeys = false
                maxAliasesForCollections = 0
                nestingDepthLimit = 20
                codePointLimit = 2_000_000
            }
            val root = Yaml(SafeConstructor(options)).load<Any>(document).mapping()
            if (root["version"] != "1.0") return null
            val metadata = root["metadata"].mapping()
            metadata["title"].text(); metadata["artist"].text()
            // The format has not defined offset semantics. Never guess and shift supplied words.
            if (metadata["offset_ms"] != null && metadata["offset_ms"].millis() != 0L) return null
            val rows = root["lines"] as? List<*> ?: return null
            if (rows.isEmpty() || rows.size > 20_000) return null
            rows.mapIndexed { index, raw ->
                val row = raw.mapping()
                val text = row["text"].text()
                if (text.length > 100_000) invalid()
                val start = row["start_ms"].millis()
                val end = row["end_ms"]?.millis()
                if (end != null && end < start) invalid()
                val line = LyricLine(index.toString(), start, text, end)
                val words = parseWords(row["words"])
                val withWords = line.copy(words = words)
                if (WordSync.valid(withWords)) withWords else line
            }.sortedBy { it.startMs }
        } catch (_: YAMLException) { null }
        catch (_: InvalidDocument) { null }
    }

    private fun parseWords(raw: Any?): List<LyricWord> = try {
        val rows = raw as? List<*> ?: emptyList<Any>()
        if (rows.size > 10_000) invalid()
        rows.map { value ->
            val word = value.mapping()
            LyricWord(word["text"].text(), word["start_ms"].millis(), word["end_ms"]?.millis())
        }
    } catch (_: InvalidDocument) { emptyList() }
}
