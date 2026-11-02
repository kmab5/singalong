package com.kmab.singalong.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import com.kmab.singalong.ui.theme.SingalongColors

object SingalongMotion {
    const val Duration = 260
    val Easing = CubicBezierEasing(.2f, 0f, 0f, 1f)
    fun <T> tween() = tween<T>(Duration, easing = Easing)
}

@Composable
fun AmbientBackground(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(SingalongColors.Background)) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.radialGradient(listOf(SingalongColors.Purple.copy(alpha = .14f), Color.Transparent),
                center = Offset(size.width * .85f, size.height * .1f), radius = size.width * .9f))
            drawRect(Brush.radialGradient(listOf(SingalongColors.PurpleText.copy(alpha = .05f), Color.Transparent),
                center = Offset(0f, size.height * .65f), radius = size.width * .8f))
        }
        content()
    }
}

@Composable
fun GlassPanel(modifier: Modifier = Modifier, opacity: Float = .88f, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    CompositionLocalProvider(LocalContentColor provides SingalongColors.Text) {
        Box(modifier.clip(shape).background(Brush.linearGradient(listOf(
            SingalongColors.Raised.copy(alpha = opacity), SingalongColors.Surface.copy(alpha = opacity))))
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .16f), Color.White.copy(alpha = .035f))), shape)) {
            content()
        }
    }
}

@Composable
fun PageHeading(title: String, subtitle: String? = null) {
    Column(Modifier.padding(top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted) }
    }
}

/** Secondary actions are visibly actionable on both glass and opaque surfaces. */
@Composable
fun ActionButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    Button(onClick, modifier.heightIn(min = 48.dp), enabled = enabled, shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SingalongColors.Raised, contentColor = SingalongColors.PurpleText),
        border = BorderStroke(1.dp, SingalongColors.Line), elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp), content = content)
}
