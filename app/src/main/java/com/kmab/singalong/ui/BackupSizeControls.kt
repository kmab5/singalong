package com.kmab.singalong.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kmab.singalong.ui.theme.SingalongColors

@Composable
internal fun BackupSizeControls(value: Long, enabled: Boolean, onChange: (Long) -> Unit) {
    var draft by rememberSaveable(value) { mutableStateOf(if (value == 0L) "50" else value.toString()) }
    val amount = draft.toLongOrNull()?.takeIf { it in 1L..Long.MAX_VALUE / (1024 * 1024) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Limit backup size", style = MaterialTheme.typography.titleSmall)
                Text(if (value == 0L) "Unlimited" else "$value MiB of uncompressed data", style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted)
            }
            Switch(value != 0L, { onChange(if (it) amount ?: 50L else 0L) }, enabled = enabled,
                modifier = Modifier.semantics { contentDescription = "Limit backup size" })
        }
        if (value != 0L) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(draft, { draft = it }, Modifier.weight(1f), label = { Text("Maximum MiB") },
                singleLine = true, enabled = enabled, isError = amount == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            ActionButton(enabled = enabled && amount != null && amount != value, onClick = { amount?.let(onChange) }) { Text("Apply") }
        }
        Text("Applies to export and restore, with no separate per-file limit. Unlimited uses available storage and memory; large backups can take longer.",
            style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted)
    }
}
