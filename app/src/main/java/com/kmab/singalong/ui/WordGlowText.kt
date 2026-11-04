package com.kmab.singalong.ui

import androidx.compose.animation.core.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import com.kmab.singalong.lyrics.WordSweep
import com.kmab.singalong.ui.theme.SingalongColors
import kotlin.math.abs
import kotlinx.coroutines.currentCoroutineContext

@Composable
fun WordGlowText(text: String, progress: Float, playing: Boolean, modifier: Modifier, style: TextStyle, alignment: TextAlign) {
    val words = remember(text) { WordSweep.words(text) }
    val sweep = remember(text) { Animatable(progress) }
    // Small clock steps interpolate; seeks and pauses immediately reflect the player's position.
    LaunchedEffect(text, progress, playing) {
        val motionScale = currentCoroutineContext()[MotionDurationScale]?.scaleFactor ?: 1f
        if (!playing || abs(progress - sweep.value) > .25f || motionScale == 0f) sweep.snapTo(progress)
        else sweep.animateTo(progress, tween(180, easing = LinearEasing))
    }
    val annotated = buildAnnotatedString {
        append(text)
        words.forEachIndexed { index, word ->
            val emphasis = WordSweep.emphasis(index, words.size, sweep.value)
            addStyle(SpanStyle(color = lerp(SingalongColors.Muted, SingalongColors.PurpleText, emphasis),
                shadow = Shadow(SingalongColors.Purple.copy(alpha = .35f * emphasis), Offset.Zero, 10f * emphasis)), word.start, word.end)
        }
    }
    Text(annotated, modifier, style = style, textAlign = alignment)
}
