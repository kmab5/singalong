package com.kmab.singalong.cards

/** One-based source rows, including gaps, make the exported excerpt identifiable. */
fun CardContent.pngFileName(selected: Set<Int>): String {
    val cleanTitle = recording.title.replace(Regex("[\\p{Cntrl}\\p{Cf}<>:\"/\\\\|?*]+"), "-")
        .replace(Regex("\\s+"), "-").trim(' ', '.', '-').ifBlank { "song" }
    val title = StringBuilder()
    var bytes = 0
    for (point in cleanTitle.codePoints().toArray()) {
        val character = String(Character.toChars(point))
        bytes += character.toByteArray(Charsets.UTF_8).size
        if (bytes > 120) break
        title.append(character)
    }
    val rows = selected.filter { it in lines.indices }.sorted().map { it + 1 }
    val ranges = mutableListOf<String>()
    var index = 0
    while (index < rows.size) {
        val first = rows[index]
        var last = first
        while (index + 1 < rows.size && rows[index + 1] == last + 1) { last = rows[++index] }
        ranges += if (first == last) "$first" else "$first-$last"
        index++
    }
    // Keep provider filenames short even for hundreds of disjoint selected lines.
    val label = ranges.joinToString(",").let { if (it.length <= 64) it else "${rows.first()}-plus-${rows.size - 1}" }
        .ifEmpty { "${initialLine + 1}" }
    return "$title-$label-singalong.png"
}
