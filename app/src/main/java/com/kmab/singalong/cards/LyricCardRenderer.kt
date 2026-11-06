package com.kmab.singalong.cards

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.graphics.ColorUtils
import com.kmab.singalong.R
import kotlin.math.min
import kotlin.math.roundToInt

data class CardRenderResult(val bitmap: Bitmap? = null, val error: String? = null)

/** One measured layout, in export coordinates, shared by previews and saved cards. */
class LyricCardRenderer(private val context: Context) {
    fun render(
        content: CardContent,
        selected: Set<Int>,
        style: CardStyle,
        cover: Bitmap? = null,
        background: Bitmap? = null,
        width: Int = style.aspect.width,
    ): CardRenderResult {
        content.selectionError(selected)?.let { return CardRenderResult(error = it) }
        if (width <= 0) return CardRenderResult(error = "Choose a valid card size.")
        val appearance = style.sanitized()
        val cardWidth = appearance.aspect.width.toFloat()
        val cardHeight = appearance.aspect.height.toFloat()
        // Never allocate beyond a native export, even if a caller supplies an excessive width.
        val outputWidth = width.coerceAtMost(appearance.aspect.width)
        val outputHeight = (outputWidth * cardHeight / cardWidth).roundToInt().coerceAtLeast(1)
        val safeCover = cover?.takeUnless { it.isRecycled }
        val safeBackground = background?.takeUnless { it.isRecycled } ?: safeCover
        val imageUsed = appearance.background == CardBackground.IMAGE && safeBackground != null
        val margin = 72f
        val innerWidth = (cardWidth - margin * 2).toInt()
        val metadata = listOf(
            content.recording.title,
            content.recording.artists.joinToString(" · "),
            content.recording.album.orEmpty(),
        )
        if (metadata.sumOf { it.length } + (content.source?.length ?: 0) > 12_000) {
            return CardRenderResult(error = "The song credits are too long to fit on a card.")
        }
        val header = metadata.mapIndexedNotNull { index, text ->
            text.takeIf { it.isNotBlank() }?.let {
                textLayout(it, innerWidth, textPaint(
                    when (index) { 0 -> 38f; 1 -> 25f; else -> 21f },
                    appearance.textColor,
                    Typeface.create("sans-serif", if (index == 0) Typeface.BOLD else Typeface.NORMAL),
                ))
            }
        }
        val headerHeight = header.sumOf { it.height } + (header.size - 1).coerceAtLeast(0) * 10f
        val credit = content.source?.takeIf { it.isNotBlank() }?.let {
            textLayout(it, innerWidth, textPaint(20f, appearance.textColor, Typeface.SANS_SERIF))
        }
        val footerHeight = 42f + if (credit != null) credit.height + 18f else 0f
        if (headerHeight > cardHeight * .30f || footerHeight > cardHeight * .22f) {
            return CardRenderResult(error = "The song credits need more room. Try a taller card.")
        }
        val footerTop = cardHeight - margin - footerHeight
        val contentArea = RectF(margin, margin + headerHeight + 60f, cardWidth - margin, footerTop - 48f)
        // Padding is also reserved for italic overhang, diacritics and the lyric surface.
        val inset = 30f
        val lyricArea = RectF(contentArea).apply { inset(inset, inset) }
        val lyricWidth = min(cardWidth * appearance.textWidth, lyricArea.width()).toInt()
        if (lyricWidth <= 0 || lyricArea.height() <= 0) return doesNotFit()
        val excerpt = content.excerpt(selected)
        val lyrics = textLayout(excerpt, lyricWidth, textPaint(appearance.fontSize, appearance.textColor,
            when (appearance.font) {
                CardFont.SANS -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
                CardFont.SERIF -> Typeface.SERIF
                CardFont.MONO -> Typeface.MONOSPACE
            }), when (appearance.alignment) {
                CardAlignment.START -> Layout.Alignment.ALIGN_NORMAL
                CardAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
                CardAlignment.END -> Layout.Alignment.ALIGN_OPPOSITE
            })
        if (lyrics.height > lyricArea.height() || lyrics.getLineEnd(lyrics.lineCount - 1) != excerpt.length) {
            return doesNotFit()
        }
        val lyricX = lyricArea.left + (lyricArea.width() - lyrics.width) * appearance.textX
        val lyricY = lyricArea.top + (lyricArea.height() - lyrics.height) * appearance.textY
        val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(outputWidth / cardWidth, outputHeight / cardHeight)
        drawBackground(canvas, cardWidth, cardHeight, appearance, safeBackground)
        val surface = readableSurface(appearance)
        // Quiet credit bands keep real metadata readable over even a busy photograph.
        drawCreditBand(canvas, RectF(40f, 40f, cardWidth - 40f, margin + headerHeight + 22f), surface, imageUsed)
        drawCreditBand(canvas, RectF(40f, footerTop - 22f, cardWidth - 40f, cardHeight - 40f), surface, imageUsed)
        if (appearance.showCover && safeCover != null && contentArea.height() > 0) {
            val side = min(cardWidth * appearance.coverSize, contentArea.height())
            val x = contentArea.left + (contentArea.width() - side) * appearance.coverX
            val y = contentArea.top + (contentArea.height() - side) * appearance.coverY
            val bounds = RectF(x, y, x + side, y + side)
            drawImage(canvas, safeCover, bounds, 22f)
            outline(canvas, bounds, 22f, withAlpha(appearance.textColor, 65), 2f)
        }
        val lyricBounds = RectF(lyricX - inset, lyricY - inset, lyricX + lyrics.width + inset, lyricY + lyrics.height + inset)
        drawLyricSurface(canvas, lyricBounds, appearance, surface)
        drawLayout(canvas, lyrics, lyricX, lyricY)
        var headerY = margin
        header.forEach { layout ->
            drawLayout(canvas, layout, margin, headerY)
            headerY += layout.height + 10f
        }
        val markSize = 40
        androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_singalong)?.mutate()?.let { mark ->
            canvas.save()
            canvas.translate(margin, footerTop)
            mark.setBounds(0, 0, markSize, markSize)
            mark.draw(canvas)
            canvas.restore()
        }
        val brand = textLayout("singalong · by kmab", innerWidth - 58,
            textPaint(24f, appearance.textColor, Typeface.create("sans-serif-medium", Typeface.NORMAL)))
        drawLayout(canvas, brand, margin + 58f, footerTop + (40f - brand.height) / 2f)
        credit?.let { drawLayout(canvas, it, margin, footerTop + 60f) }
        return CardRenderResult(bitmap = bitmap)
    }

    private fun doesNotFit() = CardRenderResult(error =
        "These lyrics do not fit at this size. Reduce text size, select fewer lines, or try a taller card.")

    private fun textPaint(size: Float, color: Int, face: Typeface) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color or (0xff shl 24)
        typeface = face
    }

    private fun textLayout(text: String, width: Int, paint: TextPaint,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(alignment)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .setIncludePad(true)
            .setUseLineSpacingFromFallbacks(true)
            .setLineSpacing(0f, 1.12f)
            .setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .build()

    private fun drawLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawBackground(canvas: Canvas, width: Float, height: Float, style: CardStyle, image: Bitmap?) {
        val base = style.backgroundColor or (0xff shl 24)
        val accent = style.accentColor or (0xff shl 24)
        canvas.drawColor(base)
        val bounds = RectF(0f, 0f, width, height)
        if (style.background == CardBackground.IMAGE && image != null) {
            drawImage(canvas, image, bounds)
            canvas.drawColor(withAlpha(Color.BLACK, (255 * style.imageShade).roundToInt()))
        } else if (style.background != CardBackground.SOLID) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, width, height,
                    intArrayOf(ColorUtils.blendARGB(base, accent, .28f), base,
                        ColorUtils.blendARGB(base, accent, .12f)), floatArrayOf(0f, .58f, 1f), Shader.TileMode.CLAMP)
            }
            canvas.drawRect(bounds, paint)
            if (style.template == CardTemplate.GLASS || style.template == CardTemplate.ARTWORK) {
                paint.shader = RadialGradient(width * .9f, height * .18f, width * .75f,
                    withAlpha(accent, 64), withAlpha(accent, 0), Shader.TileMode.CLAMP)
                canvas.drawRect(bounds, paint)
            }
        }
        when (style.template) {
            CardTemplate.GLASS -> outline(canvas, RectF(24f, 24f, width - 24f, height - 24f), 44f, withAlpha(style.textColor, 38), 1.5f)
            CardTemplate.MIDNIGHT -> canvas.drawRect(0f, 0f, 10f, height, Paint().apply { color = accent })
            CardTemplate.PAPER -> {
                val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(accent, 95); strokeWidth = 2f }
                canvas.drawLine(72f, 40f, width - 72f, 40f, ink)
                canvas.drawLine(72f, height - 40f, width - 72f, height - 40f, ink)
            }
            CardTemplate.ARTWORK -> Unit
        }
    }

    private fun readableSurface(style: CardStyle): Int {
        val text = style.textColor or (0xff shl 24)
        val base = style.backgroundColor or (0xff shl 24)
        val opposite = if (ColorUtils.calculateLuminance(text) > .35) Color.BLACK else Color.WHITE
        var surface = base
        for (step in 1..10) {
            if (ColorUtils.calculateContrast(text, surface) >= 7.0) break
            surface = ColorUtils.blendARGB(base, opposite, step / 10f)
        }
        return surface
    }

    private fun drawCreditBand(canvas: Canvas, bounds: RectF, surface: Int, imageUsed: Boolean) {
        canvas.drawRoundRect(bounds, 24f, 24f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(surface, if (imageUsed) 240 else 225)
        })
    }

    private fun drawLyricSurface(canvas: Canvas, bounds: RectF, style: CardStyle, surface: Int) {
        val radius = when (style.template) { CardTemplate.GLASS -> 30f; CardTemplate.ARTWORK -> 20f; else -> 4f }
        canvas.drawRoundRect(bounds, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(surface, 242) })
        if (style.template == CardTemplate.GLASS) {
            outline(canvas, bounds, radius, withAlpha(style.textColor, 45), 1.5f)
            canvas.drawLine(bounds.left + 28f, bounds.top, bounds.right - 28f, bounds.top,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    strokeWidth = 2f
                    shader = LinearGradient(bounds.left, 0f, bounds.right, 0f,
                        intArrayOf(withAlpha(style.accentColor, 0), withAlpha(style.accentColor, 210), withAlpha(style.accentColor, 0)),
                        null, Shader.TileMode.CLAMP)
                })
        } else if (style.template == CardTemplate.MIDNIGHT || style.template == CardTemplate.PAPER) {
            canvas.drawRect(bounds.left, bounds.top, bounds.left + 4f, bounds.bottom,
                Paint().apply { color = style.accentColor or (0xff shl 24) })
        }
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

    private fun outline(canvas: Canvas, bounds: RectF, radius: Float, color: Int, width: Float) {
        canvas.drawRoundRect(bounds, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = width
        })
    }

    private fun withAlpha(color: Int, alpha: Int) = ColorUtils.setAlphaComponent(color, alpha)
}
