package com.kmab.singalong.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmab.singalong.lyrics.GlowMotion
import com.kmab.singalong.lyrics.WordSweep
import com.kmab.singalong.ui.theme.SingalongColors
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

/** Reserve wrapping/height once at the larger size; focus and word motion affect drawing only. */
@Composable
fun AnimatedLyricText(
    original: String, normalSize: Float, currentSize: Float, current: Boolean,
    glow: Boolean, gentle: Boolean, animate: Boolean, progress: Float, playing: Boolean,
    style: TextStyle, lineSpacing: Float, alignment: TextAlign, modifier: Modifier = Modifier,
) {
    val text = original.ifBlank { "♪" }
    val reservedSize = GlowMotion.reservedSize(normalSize, currentSize)
    val fontSize = animateFloatAsState(if (current) currentSize else normalSize,
        if (animate) tween(600, easing = FastOutSlowInEasing) else snap(), label = "Lyric size")
    val focus = animateFloatAsState(if (current) 1f else 0f,
        if (animate) tween(650, easing = FastOutSlowInEasing) else snap(), label = "Lyric focus")
    val sweep = remember(text) { Animatable(progress) }
    var seconds by remember(text) { mutableFloatStateOf(0f) }
    LaunchedEffect(text, progress, playing) {
        if (!playing || abs(progress - sweep.value) > .25f) sweep.snapTo(progress)
        else sweep.animateTo(progress, tween(180, easing = LinearEasing))
    }
    LaunchedEffect(text, current, playing, glow, animate) {
        if (!current || !playing || !glow || !animate) return@LaunchedEffect
        val context = currentCoroutineContext()
        snapshotFlow { context[MotionDurationScale]?.scaleFactor ?: 1f }.collectLatest { factor ->
            if (factor > 0f) {
                var previous = withFrameNanos { it }
                while (currentCoroutineContext().isActive) withFrameNanos { now ->
                    seconds += ((now - previous) / 1_000_000_000f).coerceAtMost(.1f) / factor
                    previous = now
                }
            }
        }
    }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 64)
    BoxWithConstraints(modifier.fillMaxWidth().semantics { this.text = AnnotatedString(original) }) {
        val gutter = with(density) { 8.dp.toPx() }
        val width = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(1)
        val constraints = Constraints(minWidth = (width - 2 * gutter.toInt()).coerceAtLeast(1),
            maxWidth = (width - 2 * gutter.toInt()).coerceAtLeast(1))
        val layoutStyle = style.copy(fontSize = reservedSize.sp, lineHeight = (reservedSize * lineSpacing).sp,
            color = Color.Unspecified, shadow = null, textAlign = alignment)
        val layout = measurer.measure(text, layoutStyle, constraints = constraints)
        // Mask other ranges, rather than re-laying out individual words: wrapping, RTL shaping,
        // punctuation, explicit whitespace and the accessible sentence remain intact.
        val words = remember(text) { WordSweep.words(text) }
        val masks = remember(text, layout.multiParagraph, layoutStyle, constraints, glow) {
            if (!glow) emptyList() else words.map { word ->
                val masked = buildAnnotatedString {
                    append(text)
                    val hidden = SpanStyle(color = Color.Transparent, shadow = Shadow.None)
                    if (word.start > 0) addStyle(hidden, 0, word.start)
                    if (word.end < text.length) addStyle(hidden, word.end, text.length)
                }
                measurer.measure(masked, layoutStyle, constraints = constraints) to
                    layout.getPathForRange(word.start, word.end).getBounds().center
            }
        }
        val height = with(density) { (layout.size.height + gutter * 2).toDp() }
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val amount = focus.value
            // Android's large-font scaling is nonlinear; convert both SP sizes before scaling pixels.
            val ratio = fontSize.value.sp.toPx() / reservedSize.sp.toPx() * if (gentle) (1f + .02f * amount) else 1f
            val pivotX = when (alignment) {
                TextAlign.Center -> layout.size.width / 2f
                TextAlign.End -> if (layout.layoutInput.layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) layout.size.width.toFloat() else 0f
                else -> if (layout.layoutInput.layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) 0f else layout.size.width.toFloat()
            }
            translate(gutter, gutter) {
                scale(ratio, ratio, Offset(pivotX, layout.size.height / 2f)) {
                    if (!glow || amount == 0f) drawText(layout, color = style.color, shadow = style.shadow)
                    else masks.forEachIndexed { index, (mask, pivot) ->
                        val emphasis = WordSweep.emphasis(index, words.size, sweep.value) * amount
                        val motion = GlowMotion.transform(index, seconds, emphasis)
                        translate(motion.xDp.dp.toPx(), motion.yDp.dp.toPx()) {
                            scale(motion.scale, motion.scale, pivot) {
                                drawText(mask, color = lerp(style.color, SingalongColors.Text, emphasis),
                                    shadow = Shadow(SingalongColors.Purple.copy(alpha = .85f * emphasis), Offset.Zero, 16.dp.toPx() * emphasis))
                            }
                        }
                    }
                }
            }
        }
    }
}
