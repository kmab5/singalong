package com.kmab.singalong.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import com.kmab.singalong.lyrics.FloatingWordTracking
import com.kmab.singalong.lyrics.PlaybackClock
import com.kmab.singalong.model.LyricLine
import com.kmab.singalong.model.PlaybackSnapshot
import com.kmab.singalong.model.PlaybackStatus
import com.kmab.singalong.ui.theme.SingalongColors
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/** One intact sentence preserves wrapping, shaping, punctuation and accessibility. */
@Composable
internal fun FloatingLyricText(
    line: LyricLine, lineEndMs: Long?, enabled: Boolean, current: Boolean,
    playback: PlaybackSnapshot?, delayMs: Long, style: TextStyle, alignment: TextAlign,
    modifier: Modifier = Modifier,
) {
    val tracking = remember(line, lineEndMs, enabled, current) {
        FloatingWordTracking.create(line, lineEndMs, enabled, current)
    }
    if (tracking == null) {
        Text(line.text.ifBlank { "♪" }, modifier.fillMaxWidth(), style = style, textAlign = alignment)
    } else {
        TimedFloatingText(line.text, tracking, playback, delayMs, style, alignment, modifier)
    }
}

@Composable
private fun TimedFloatingText(
    original: String, tracking: FloatingWordTracking, playback: PlaybackSnapshot?, delayMs: Long,
    style: TextStyle, alignment: TextAlign, modifier: Modifier,
) {
    var position by remember(playback, tracking) {
        mutableStateOf(PlaybackClock.position(playback, SystemClock.elapsedRealtime()))
    }
    // Only the current followed line mounts this clock. Disabling words/manual reading removes
    // this composable and cancels it. Paused players update once; no decorative animation loop.
    LaunchedEffect(playback, tracking) {
        if (playback?.status != PlaybackStatus.PLAYING) return@LaunchedEffect
        while (currentCoroutineContext().isActive) withFrameNanos {
            position = PlaybackClock.position(playback, SystemClock.elapsedRealtime())
        }
    }
    val text = buildAnnotatedString {
        append(original)
        tracking.ranges.forEachIndexed { index, range ->
            addStyle(SpanStyle(color = lerp(SingalongColors.Muted, SingalongColors.Text,
                tracking.emphasis(index, position, delayMs))), range.start, range.end)
        }
    }
    // Word color follows real time even with Android animations disabled. Font size, position
    // and shadow remain unchanged, so reduced motion never loses timestamp information.
    Text(text, modifier.fillMaxWidth(), style = style, textAlign = alignment)
}
