package com.kmab.singalong.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val colors = darkColorScheme(
    primary = SingalongColors.Purple,
    onPrimary = SingalongColors.Background,
    primaryContainer = SingalongColors.Raised,
    onPrimaryContainer = SingalongColors.PurpleText,
    secondary = SingalongColors.Green,
    background = SingalongColors.Background,
    onBackground = SingalongColors.Text,
    surface = SingalongColors.Surface,
    onSurface = SingalongColors.Text,
    surfaceVariant = SingalongColors.Raised,
    onSurfaceVariant = SingalongColors.Muted,
    outline = SingalongColors.Muted,
    outlineVariant = SingalongColors.Line,
    error = SingalongColors.Critical,
)

@Composable
fun SingalongTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = SingalongTypography, content = content)
}
