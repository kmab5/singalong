package com.kmab.singalong.cards

import android.graphics.RectF
import android.text.Layout
import android.text.TextPaint
import org.junit.Assert.*
import org.junit.Test

class CardTextFlowTest {
    @Test fun wrappingAtTheBottomDoesNotLeaveIndentationOnRowsAboveTheMark() {
        for (left in listOf(true, false)) {
            val box = RectF(0f, 0f, 500f, 800f)
            val logo = RectF(if (left) 0f else 360f, 780f, if (left) 140f else 500f, 800f)
            val block = requireNotNull(CardTextFlow.fit("Original words that wrap across multiple rows. ".repeat(8),
                box, TextPaint().apply { textSize = 40f }, 26f,
                if (left) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_OPPOSITE, 1.18f, 1f, logo))
            assertEquals(800f, block.y + block.layout.height, .1f)
            for (line in 0 until block.layout.lineCount) {
                if (block.y + block.layout.getLineBottom(line) <= logo.top) {
                    if (left) assertEquals(0f, block.layout.getLineLeft(line), 1f)
                    else assertEquals(500f, block.layout.getLineRight(line), 1f)
                }
            }
        }
    }

    @Test fun bottomAlignedVerseSharesTheLogoRowAndOnlyNearbyRowsWrap() {
        for (left in listOf(true, false)) for (bias in listOf(0f, 1f)) {
            val box = RectF(0f, 0f, 500f, 600f)
            val logo = RectF(if (left) 0f else 380f, if (bias == 0f) 0f else 520f,
                if (left) 120f else 500f, if (bias == 0f) 80f else 600f)
            val text = "The first original line\nMore words in the middle\nOne last original line"
            val block = requireNotNull(CardTextFlow.fit(text, box, TextPaint().apply { textSize = 40f },
                30f, Layout.Alignment.ALIGN_NORMAL, 1.18f, bias, logo))
            assertEquals(text, block.layout.text.toString())
            if (bias == 1f) assertEquals(600f, block.y + block.layout.height, .1f)
            for (line in 0 until block.layout.lineCount) {
                if (block.y + block.layout.getLineBottom(line) > logo.top && block.y + block.layout.getLineTop(line) < logo.bottom) {
                    if (left) assertTrue(block.layout.getLineLeft(line) >= logo.right - 1)
                    else assertTrue(block.layout.getLineRight(line) <= logo.left + 1)
                }
            }
        }
    }
}
