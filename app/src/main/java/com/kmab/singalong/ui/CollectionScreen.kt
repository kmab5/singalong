package com.kmab.singalong.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmab.singalong.model.LibraryEntry
import com.kmab.singalong.library.LibrarySort
import com.kmab.singalong.ui.theme.SingalongColors
import java.text.DateFormat
import java.util.Date

@Composable
fun ColumnScope.CollectionScreen(entries: List<LibraryEntry>, history: Boolean, onOpen: (String) -> Unit,
    onAdd: () -> Unit, onFavorite: (LibraryEntry) -> Unit,
    sort: LibrarySort = LibrarySort.TITLE, onSort: (LibrarySort) -> Unit = {}, onScanFolder: () -> Unit = {}, scanning: Boolean = false) {
    var query by rememberSaveable { mutableStateOf("") }
    var favorites by rememberSaveable { mutableStateOf(false) }
    var historySort by rememberSaveable { mutableStateOf(LibrarySort.RECENT) }
    var sorting by remember { mutableStateOf(false) }
    val selectedSort = if (history) historySort else sort
    val rows = selectedSort.sort(entries.filter { (if (history) it.playCount > 0 else it.hasLyrics) && (!favorites || it.favorite) &&
        (it.recording.title + " " + it.recording.artists.joinToString()).contains(query, true) }
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { PageHeading(if (history) "Recently played" else "Your lyric library", if (history) "The songs you've spent time with." else "Saved words, ready offline.") }
        FilledTonalIconButton(onClick = onAdd, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Add, "Add a song") }
        if (!history) FilledTonalIconButton(onClick = onScanFolder, modifier = Modifier.size(48.dp)) {
            Icon(if (scanning) Icons.Outlined.Sync else Icons.Outlined.FolderOpen, if (scanning) "View folder scan progress" else "Find lyrics in a folder")
        }
    }
    OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), label = { Text("Search songs or artists") },
        leadingIcon = { Icon(Icons.Outlined.Search, null) }, trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear search") } })
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        FilterChip(favorites, onClick = { favorites = !favorites }, label = { Text("Favorites") }, leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(16.dp)) })
        Text("${rows.size} songs", style = MaterialTheme.typography.labelMedium, color = SingalongColors.Muted)
    }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { sorting = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Outlined.Sort, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text("Sort: ${selectedSort.label}", Modifier.weight(1f)); Icon(Icons.Outlined.ExpandMore, null)
        }
        DropdownMenu(sorting, { sorting = false }) { LibrarySort.entries.forEach { order ->
            DropdownMenuItem(text = { Text(order.label) }, leadingIcon = { RadioButton(selectedSort == order, onClick = null) }, onClick = {
                sorting = false; if (history) historySort = order else onSort(order)
            })
        } }
    }
    if (!history && scanning) ActionButton(onClick = onScanFolder, modifier = Modifier.fillMaxWidth()) { Text("Folder scan in progress · view details") }
    if (rows.isEmpty()) GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.LibraryMusic, null, tint = SingalongColors.Purple)
            Text(if (query.isNotBlank() || favorites) "No songs match these filters." else "Your next song starts the collection.", style = MaterialTheme.typography.titleMedium)
            Text(if (history) "Listen for ten seconds in a selected app to start your history."
                else "Play a song, find lyrics in a music folder, or add lyrics yourself.", color = SingalongColors.Muted)
            if (!history && query.isBlank() && !favorites) ActionButton(onClick = onScanFolder) { Text("Find lyrics in a folder") }
            ActionButton(onClick = onAdd) { Text("Add a song") }
        }
    } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        items(rows, key = { it.recording.id }) { entry ->
            GlassPanel(Modifier.animateItem().fillMaxWidth().clickable(onClickLabel = "Read ${entry.recording.title}") { onOpen(entry.recording.id) }) {
                Row(Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (entry.hasLyrics) Icons.Outlined.Lyrics else Icons.Outlined.MusicNote, null, Modifier.size(24.dp), tint = SingalongColors.PurpleText)
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(entry.recording.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(entry.recording.artists.joinToString(), style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (history) "${entry.playCount} plays · ${entry.lastPlayedAtMs?.let { DateFormat.getDateInstance(DateFormat.SHORT).format(Date(it)) }.orEmpty()}"
                            else "Available offline", style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted)
                    }
                    IconToggleButton(checked = entry.favorite, onCheckedChange = { onFavorite(entry) }) {
                        Icon(if (entry.favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            if (entry.favorite) "Remove from favorites" else "Add to favorites",
                            tint = if (entry.favorite) SingalongColors.Purple else SingalongColors.Muted)
                    }
                }
            }
        }
    }
}
