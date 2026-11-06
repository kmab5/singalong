package com.kmab.singalong.cards

import android.graphics.Canvas
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import kotlin.math.ceil

internal data class CardTextBlock(val layout: StaticLayout, val x: Float, val y: Float) {
    fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }
}

/** Measures physical left/right indents per visual row, including RTL and wrapped source lines. */
internal object CardTextFlow {
    fun fit(text: CharSequence, box: RectF, paint: TextPaint, minimumSize: Float,
        alignment: Layout.Alignment, spacing: Float, bias: Float, exclusion: RectF): CardTextBlock? {
        if (text.isBlank() || box.width() < 1 || box.height() < 1) return null
        val measuredPaint = TextPaint(paint)
        while (true) {
            for (strategy in intArrayOf(android.graphics.text.LineBreaker.BREAK_STRATEGY_HIGH_QUALITY,
                android.graphics.text.LineBreaker.BREAK_STRATEGY_SIMPLE)) {
                measure(text, box, measuredPaint, alignment, spacing, bias, exclusion, strategy)?.let { return it }
            }
            if (measuredPaint.textSize <= minimumSize) return null
            measuredPaint.textSize = (measuredPaint.textSize - 2f).coerceAtLeast(minimumSize)
        }
    }

    private fun measure(text: CharSequence, box: RectF, paint: TextPaint, alignment: Layout.Alignment,
        spacing: Float, bias: Float, exclusion: RectF, strategy: Int): CardTextBlock? {
        var left = IntArray(1)
        var right = IntArray(1)
        val seen = mutableSetOf<Pair<List<Int>, List<Int>>>()
        // Reflow changes physical row positions. Recompute rather than retain stale indents.
        // A cycle tries simpler line breaking, then a smaller size; never reserve unrelated rows.
        repeat(24) {
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, box.width().toInt())
                .setAlignment(alignment).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .setIncludePad(true).setUseLineSpacingFromFallbacks(true).setLineSpacing(0f, spacing)
                .setBreakStrategy(strategy)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE).setIndents(left, right).build()
            if (layout.height > box.height()) return null
            val y = box.top + (box.height() - layout.height) * bias
            // An extra zero prevents StaticLayout from repeating the last row's indent forever.
            val nextLeft = IntArray(layout.lineCount + 1)
            val nextRight = IntArray(layout.lineCount + 1)
            for (line in 0 until layout.lineCount) {
                val insets = CardTextExclusion.insets(box.left, box.right,
                    y + layout.getLineTop(line), y + layout.getLineBottom(line),
                    exclusion.left, exclusion.top, exclusion.right, exclusion.bottom)
                val l = ceil(insets.first).toInt()
                val r = ceil(insets.second).toInt()
                if (l + r >= box.width() - 1) return null
                nextLeft[line] = l
                nextRight[line] = r
            }
            if (nextLeft.contentEquals(left) && nextRight.contentEquals(right)) return CardTextBlock(layout, box.left, y)
            if (!seen.add(nextLeft.toList() to nextRight.toList())) return null
            left = nextLeft
            right = nextRight
        }
        return null
    }
}
