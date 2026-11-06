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

    fun render(content: CardContent, selected: Set<Int>, style: CardStyle, cover: Bitmap? = null,
        background: Bitmap? = null, width: Int = style.aspect.width): CardRenderResult {
        content.selectionError(selected)?.let { return CardRenderResult(error = it) }
        if (width <= 0) return CardRenderResult(error = "Choose a valid card size.")
        val w = style.aspect.width.toFloat()
        val h = style.aspect.height.toFloat()
        val outputWidth = width.coerceAtMost(style.aspect.width)
        val outputHeight = (outputWidth * h / w).roundToInt().coerceAtLeast(1)
        val safeCover = cover?.takeUnless { it.isRecycled }?.takeIf { style.showCover }
        val photo = background?.takeUnless { it.isRecycled } ?: safeCover
        val layout = CardComposition.create(style.template, style.aspect, safeCover != null)
        fun CardRegion.bounds() = RectF(x * w, y * h, right * w, bottom * h)
        val ink = style.palette.ink
        val alignment = when (style.alignment) {
            CardAlignment.START -> Layout.Alignment.ALIGN_NORMAL
            CardAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
            CardAlignment.END -> Layout.Alignment.ALIGN_OPPOSITE
        }
        val metadata = content.metadata(style.details)
        if (metadata.sumOf { it.second.length } > 12_000) return CardRenderResult(error = "The selected song details are too long for a card. Hide a detail to continue.")
        val title = metadata.firstOrNull { it.first == CardMetadata.TITLE }?.second.orEmpty()
        val details = metadata.filter { it.first != CardMetadata.TITLE }.joinToString("\n") { it.second }
        val titleBox = layout.title.bounds()
        val detailBox = layout.details.bounds()
        val lyricBox = layout.lyrics.bounds()
        val centered = style.template == CardTemplate.GALLERY
        val titleLayout = fit(title, titleBox, 38f, 24f, ink, faces.getValue(CardFont.INTER), if (centered) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
        val detailLayout = fit(details, detailBox, 25f, 18f, ink, faces.getValue(CardFont.INTER))
        if ((title.isNotBlank() && titleLayout == null) || (details.isNotBlank() && detailLayout == null))
            return CardRenderResult(error = "The selected details need more room. Hide a detail or choose a taller format.")
        val size = style.textSize.pixels * if (style.font == CardFont.CAVEAT) 1.2f else 1f
        val lyrics = fit(content.excerpt(selected), lyricBox, size, size * .72f, ink, faces.getValue(style.font), alignment)
            ?: return CardRenderResult(error = "These lyrics do not fit. Choose Small text, fewer lines, or a taller format.")
        val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(outputWidth / w, outputHeight / h)
        drawBackground(canvas, w, h, style, photo)
        drawCompositionDetails(canvas, w, h, style, layout)
        layout.cover?.let { region -> safeCover?.let { art ->
            val bounds = region.bounds()
            if (style.template in setOf(CardTemplate.POSTCARD, CardTemplate.GALLERY)) {
                val matte = RectF(bounds).apply { inset(-12f, -12f) }
                canvas.drawRoundRect(matte, 5f, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.palette.ink; alpha = 24 })
            }
            drawImage(canvas, art, bounds, if (style.template == CardTemplate.GLASS) 26f else 8f)
        } }
        titleLayout?.let { drawLayout(canvas, it, titleBox.left, titleBox.top) }
        detailLayout?.let { drawLayout(canvas, it, detailBox.left, detailBox.top) }
        val lyricY = lyricBox.top + (lyricBox.height() - lyrics.height) * if (style.template == CardTemplate.MIDNIGHT) 0f else .5f
        drawLayout(canvas, lyrics, lyricBox.left, lyricY)
        drawBrand(canvas, layout.brand.bounds(), style, ink)
        return CardRenderResult(bitmap = bitmap)
    }

    /** No ellipsis, slicing or hidden lines: the whole excerpt fits or an explicit error is returned. */
    private fun fit(text: String, box: RectF, initialSize: Float, minimumSize: Float, color: Int, face: Typeface,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout? {
        if (text.isBlank() || box.width() < 1) return null
        var size = initialSize
        while (true) {
            val layout = textLayout(text, box.width().toInt(), textPaint(size, color, face), alignment)
            if (layout.height <= box.height() && layout.getLineEnd(layout.lineCount - 1) == text.length) return layout
            if (size <= minimumSize) return null
            size = (size - 2f).coerceAtLeast(minimumSize)
        }
    }

    private fun drawBrand(canvas: Canvas, bounds: RectF, style: CardStyle, color: Int) {
        val side = minOf(38f, bounds.height())
        ContextCompat.getDrawable(context, R.drawable.ic_singalong)?.mutate()?.let { icon ->
            canvas.save()
            canvas.translate(bounds.left, bounds.top)
            icon.setBounds(0, 0, side.toInt(), side.toInt())
            icon.draw(canvas)
            canvas.restore()
        }
        if (style.showAppName) {
            val box = RectF(bounds.left + side + 10f, bounds.top, bounds.right, bounds.bottom)
            fit("singalong", box, 22f, 14f, color, faces.getValue(CardFont.INTER))?.let {
                drawLayout(canvas, it, box.left, box.top + (side - it.height) / 2f)
            }
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
            canvas.drawColor(ColorUtils.setAlphaComponent(base, 220))
        } else if (style.background == CardBackground.GRADIENT) {
            paint.shader = RadialGradient(w * .9f, h * .12f, w,
                ColorUtils.blendARGB(base, accent, .32f), base, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, paint)
        }
    }

    private fun drawCompositionDetails(canvas: Canvas, w: Float, h: Float, style: CardStyle, layout: CardComposition) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.palette.accent; strokeWidth = 2f }
        when (style.template) {
            CardTemplate.GLASS -> {
                paint.style = Paint.Style.STROKE; paint.alpha = 45
                canvas.drawCircle(w * .87f, h * .25f, w * .45f, paint)
                canvas.drawCircle(w * .87f, h * .25f, w * .52f, paint)
            }
            CardTemplate.MIDNIGHT -> {
                canvas.drawRect(w * .08f, h * .215f, w * .19f, h * .222f, paint)
                paint.alpha = 65
                canvas.drawLine(w * .08f, h * .735f, w * .92f, h * .735f, paint)
            }
            CardTemplate.PAPER -> {
                paint.alpha = 100
                canvas.drawLine(w * .14f, h * .34f, w * .14f, h * .90f, paint)
            }
            CardTemplate.ARTWORK -> Unit
            CardTemplate.SLEEVE -> {
                paint.alpha = 85
                canvas.drawLine(w * .08f, h * .455f, w * .92f, h * .455f, paint)
            }
            CardTemplate.POSTCARD -> {
                paint.alpha = 70; paint.pathEffect = DashPathEffect(floatArrayOf(6f, 10f), 0f)
                canvas.drawLine(w * .08f, h * .445f, w * .92f, h * .445f, paint)
            }
            CardTemplate.DUET -> {
                if (layout.cover != null) { paint.alpha = 80; canvas.drawLine(w * .47f, h * .10f, w * .47f, h * .91f, paint) }
            }
            CardTemplate.GALLERY -> {
                paint.alpha = 90
                canvas.drawLine(w * .46f, h * .81f, w * .54f, h * .81f, paint)
            }
        }
    }
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
