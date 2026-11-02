package com.kmab.singalong.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.kmab.singalong.ui.theme.SingalongColors
import com.kmab.singalong.ui.theme.SingalongTheme

/** Original sample copy, explicitly a layout preview, never passed off as fetched lyrics. */
@Composable
fun PreviewLyricsPanel(
    bilingual: Boolean,
    modifier: Modifier = Modifier,
    dragModifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    textSize: Float = 22f,
    translationSize: Float = 16f,
) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = SingalongColors.Surface, tonalElevation = 0.dp) {
        Column(Modifier.padding(start = 20.dp, end = 12.dp, bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(dragModifier.heightIn(min = 48.dp).weight(1f), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DragHandle, contentDescription = "Move lyrics panel", tint = SingalongColors.Muted)
                    Spacer(Modifier.width(8.dp))
                    Text("layout preview", style = MaterialTheme.typography.labelMedium, color = SingalongColors.Muted)
                }
                if (onClose != null) IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close lyrics preview")
                }
            }
            Row(Modifier.fillMaxWidth().padding(end = 8.dp)) {
                Box(Modifier.padding(top = 5.dp).width(3.dp).height(if (bilingual) 78.dp else 54.dp).clip(RoundedCornerShape(2.dp)).background(SingalongColors.Purple))
                Column(Modifier.padding(start = 16.dp).weight(1f)) {
                    Text(if (bilingual) "La noche guarda\nnuestra voz." else "Let the next line\nfind you here.", style = MaterialTheme.typography.titleLarge, fontSize = textSize.sp, lineHeight = (textSize * 1.4f).sp)
                    if (bilingual) {
                        Spacer(Modifier.height(8.dp))
                        Text("The night holds our voice.", fontSize = translationSize.sp, lineHeight = (translationSize * 1.4f).sp, color = SingalongColors.PurpleText)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("original sample · no song lyrics loaded", style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted)
        }
    }
}

@Preview(name = "compact lyrics", widthDp = 344, showBackground = true, backgroundColor = 0xFF0E0E11)
@Composable private fun CompactPreview() = SingalongTheme { PreviewLyricsPanel(false) }

@Preview(name = "bilingual lyrics", widthDp = 344, showBackground = true, backgroundColor = 0xFF0E0E11)
@Preview(name = "large text", widthDp = 344, fontScale = 1.5f)
@Composable private fun BilingualPreview() = SingalongTheme { PreviewLyricsPanel(true) }
