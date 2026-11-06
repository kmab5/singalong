package com.kmab.singalong.cards

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.kmab.singalong.R
import kotlin.math.roundToInt

data class CardRenderResult(val bitmap: Bitmap? = null, val error: String? = null)

/** Preview and export use the same measured composition and bundled typefaces. */
class LyricCardRenderer(private val context: Context) {
    private val faces = CardFont.entries.associateWith { ResourcesCompat.getFont(context, it.resource) ?: Typeface.DEFAULT }

    fun render(content: CardContent, selected: Set<Int>, requestedStyle: CardStyle, cover: Bitmap? = null,
        background: Bitmap? = null, width: Int = requestedStyle.aspect.width): CardRenderResult {
        content.selectionError(selected)?.let { return CardRenderResult(error = it) }
        if (width <= 0) return CardRenderResult(error = "Choose a valid card size.")
        val style = requestedStyle.sanitized()
        val w = style.aspect.width.toFloat()
        val h = style.aspect.height.toFloat()
        val outputWidth = width.coerceAtMost(style.aspect.width)
        val outputHeight = (outputWidth * h / w).roundToInt().coerceAtLeast(1)
        val safeCover = cover?.takeUnless { it.isRecycled }?.takeIf { style.showCover }
        val photo = background?.takeUnless { it.isRecycled } ?: safeCover
        val layout = CardComposition.create(style, safeCover != null)
        fun CardRegion.bounds() = RectF(x * w, y * h, right * w, bottom * h)
        val ink = style.palette.ink
        val alignment = when (style.alignment) {
            CardAlignment.START -> Layout.Alignment.ALIGN_NORMAL
            CardAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
            CardAlignment.END -> Layout.Alignment.ALIGN_OPPOSITE
        }
        val metadata = content.metadata(style.details)
        if (metadata.sumOf { it.second.length } > 12_000) return CardRenderResult(error = "The selected song details are too long for a card. Hide a detail to continue.")
        val creditBox = layout.credits.bounds()
        val lyricBox = layout.lyrics.bounds()
        val credits = measureCredits(metadata, creditBox, ink, layout.centeredCredits)
            ?: return CardRenderResult(error = "The selected details need more room. Hide a detail or choose a taller format.")
        val size = style.fontSize * if (style.font == CardFont.CAVEAT) 1.14f else 1f
        val face = Typeface.create(faces.getValue(style.font), style.weight.value, false)
        val lyrics = fit(content.excerpt(selected), lyricBox, size, size * .64f, ink, face, alignment, style.spacing.multiplier)
            ?: return CardRenderResult(error = "These lyrics do not fit. Reduce text size, select fewer lines, or choose a taller format.")
        val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(outputWidth / w, outputHeight / h)
        drawBackground(canvas, w, h, style, photo)
        layout.cover?.let { region -> safeCover?.let { art ->
            val bounds = region.bounds()
            val radius = when (style.corners) {
                CardCorners.SQUARE -> 0f
                CardCorners.SOFT -> minOf(bounds.width(), bounds.height()) * .055f
                CardCorners.ROUND -> minOf(bounds.width(), bounds.height()) / 2f
            }
            drawImage(canvas, art, bounds, radius)
        } }
        var creditY = creditBox.top + (creditBox.height() - credits.sumOf { it.height } - (credits.size - 1).coerceAtLeast(0) * 10f) / 2f
        credits.forEach { credit ->
            drawLayout(canvas, credit, creditBox.left, creditY)
            creditY += credit.height + 10f
        }
        val lyricY = lyricBox.top + (lyricBox.height() - lyrics.height) * style.placement.bias
        drawLayout(canvas, lyrics, lyricBox.left, lyricY)
        drawBrand(canvas, layout.brand.bounds(), style, ink)
        return CardRenderResult(bitmap = bitmap)
    }

    private fun measureCredits(metadata: List<Pair<CardMetadata, String>>, box: RectF, color: Int, centered: Boolean): List<StaticLayout>? {
        val title = metadata.firstOrNull { it.first == CardMetadata.TITLE }?.second
        val music = metadata.filter { it.first in setOf(CardMetadata.ARTIST, CardMetadata.ALBUM) }.joinToString(" · ") { it.second }
        val details = (listOf(music) + metadata.filter { it.first in setOf(CardMetadata.FILE_NAME, CardMetadata.LOCATION) }.map { it.second })
            .filter { it.isNotBlank() }.joinToString("\n")
        val alignment = if (centered) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL
        for (step in 0..8) {
            val scale = 1f - step * .05f
            val rows = listOfNotNull(
                title?.let { textLayout(it, box.width().toInt(), textPaint(44f * scale, color,
                    Typeface.create(faces.getValue(CardFont.INTER), 600, false)), alignment) },
                details.takeIf { it.isNotBlank() }?.let { textLayout(it, box.width().toInt(), textPaint(32f * scale, color,
                    faces.getValue(CardFont.INTER)), alignment) },
            )
            if (rows.sumOf { it.height } + (rows.size - 1).coerceAtLeast(0) * 10 <= box.height()) return rows
        }
        return null
    }

    /** No ellipsis, slicing or hidden lines: the whole excerpt fits or an explicit error is returned. */
    private fun fit(text: String, box: RectF, initialSize: Float, minimumSize: Float, color: Int, face: Typeface,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL, spacing: Float = 1.18f): StaticLayout? {
        if (text.isBlank() || box.width() < 1) return null
        var size = initialSize
        while (true) {
            val layout = textLayout(text, box.width().toInt(), textPaint(size, color, face), alignment, spacing)
            if (layout.height <= box.height() && layout.getLineEnd(layout.lineCount - 1) == text.length) return layout
            if (size <= minimumSize) return null
            size = (size - 2f).coerceAtLeast(minimumSize)
        }
    }

    private fun drawBrand(canvas: Canvas, bounds: RectF, style: CardStyle, color: Int) {
        val markWidth = minOf(56f, bounds.height() * 26f / 24f)
        val markHeight = markWidth * 24f / 26f
        val namePaint = textPaint(28f, color, faces.getValue(CardFont.INTER))
        val nameWidth = if (style.showAppName) namePaint.measureText("singalong") else 0f
        val gap = if (style.showAppName) 16f else 0f
        val x = if (style.footer == CardFooter.END) bounds.right - markWidth - gap - nameWidth else bounds.left
        ContextCompat.getDrawable(context, R.drawable.ic_singalong_mark)?.mutate()?.let { icon ->
            icon.setTint(color)
            canvas.save()
            canvas.translate(x, bounds.top + (bounds.height() - markHeight) / 2f)
            icon.setBounds(0, 0, markWidth.toInt(), markHeight.toInt())
            icon.draw(canvas)
            canvas.restore()
        }
        if (style.showAppName) {
            val baseline = bounds.centerY() - (namePaint.ascent() + namePaint.descent()) / 2f
            canvas.drawText("singalong", x + markWidth + gap, baseline, namePaint)
        }
    }

    private fun drawBackground(canvas: Canvas, w: Float, h: Float, style: CardStyle, photo: Bitmap?) {
        val base = style.palette.base
        val accent = style.palette.accent
        canvas.drawColor(base)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (style.background == CardBackground.IMAGE && photo != null) {
            drawImage(canvas, photo, RectF(0f, 0f, w, h))
            // One continuous veil keeps all text readable without black text boxes.
            canvas.drawColor(ColorUtils.setAlphaComponent(base, (style.photoShade * 255).roundToInt()))
        } else if (style.background == CardBackground.GRADIENT) {
            paint.shader = RadialGradient(w * .9f, h * .12f, w,
                ColorUtils.blendARGB(base, accent, .14f), base, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, paint)
        }
    }

    private fun textPaint(size: Float, color: Int, face: Typeface) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color or (0xff shl 24)
        typeface = face
    }

    private fun textLayout(text: String, width: Int, paint: TextPaint,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL, spacing: Float = 1.18f): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(alignment)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .setIncludePad(true)
            .setUseLineSpacingFromFallbacks(true)
            .setLineSpacing(0f, spacing)
            .setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .build()

    private fun drawLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    /** Center crop directly from the source: no intermediate full-size bitmaps. */
    private fun drawImage(canvas: Canvas, bitmap: Bitmap, bounds: RectF, radius: Float = 0f) {
        val sourceRatio = bitmap.width.toFloat() / bitmap.height
        val targetRatio = bounds.width() / bounds.height()
        val source = if (sourceRatio > targetRatio) {
            val cropWidth = (bitmap.height * targetRatio).roundToInt().coerceAtLeast(1)
            val left = (bitmap.width - cropWidth) / 2
            Rect(left, 0, left + cropWidth, bitmap.height)
        } else {
            val cropHeight = (bitmap.width / targetRatio).roundToInt().coerceAtLeast(1)
            val top = (bitmap.height - cropHeight) / 2
            Rect(0, top, bitmap.width, top + cropHeight)
        }
        canvas.save()
        if (radius > 0) canvas.clipPath(Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) })
        canvas.drawBitmap(bitmap, source, bounds, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
    }

}
