package com.kmab.singalong.cards

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
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
        background: Bitmap? = null, width: Int = requestedStyle.aspect.width, emphasized: Set<Int> = emptySet()): CardRenderResult {
        content.selectionError(selected)?.let { return CardRenderResult(error = it) }
        if (width <= 0) return CardRenderResult(error = "Choose a valid card size.")
        val style = requestedStyle.sanitized()
        val w = style.aspect.width.toFloat()
        val h = style.aspect.height.toFloat()
        val outputWidth = width.coerceAtMost(style.aspect.width)
        val outputHeight = (outputWidth * h / w).roundToInt().coerceAtLeast(1)
        val albumArt = cover?.takeUnless { it.isRecycled }
        val safeCover = albumArt?.takeIf { style.showCover }
        val photo = background?.takeUnless { it.isRecycled } ?: albumArt
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
        val mark = layout.brand.bounds()
        val exclusion = RectF(mark).apply { inset(-16f, -12f) }
        val creditText = creditText(metadata)
        val credits = if (creditText.isEmpty()) null else CardTextFlow.fit(creditText, creditBox,
            textPaint(44f, ink, faces.getValue(CardFont.INTER)), 26.4f,
            if (layout.centeredCredits) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL,
            1.18f, .5f, exclusion)
        if (creditText.isNotEmpty() && credits == null) return CardRenderResult(error = "The selected details need more room. Hide a detail or choose a taller format.")
        val size = style.fontSize * if (style.font == CardFont.CAVEAT) 1.14f else 1f
        val face = Typeface.create(faces.getValue(style.font), style.weight.value, false)
        val text = emphasizedText(content, selected, emphasized, style.palette.accent)
        val lyrics = CardTextFlow.fit(text, lyricBox, textPaint(size, ink, face), size * .64f,
            alignment, style.spacing.multiplier, style.placement.bias, exclusion)
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
        credits?.draw(canvas)
        lyrics.draw(canvas)
        drawBrand(canvas, mark, style, ink)
        return CardRenderResult(bitmap = bitmap)
    }

    private fun creditText(metadata: List<Pair<CardMetadata, String>>): CharSequence {
        val text = SpannableStringBuilder()
        metadata.firstOrNull { it.first == CardMetadata.TITLE }?.second?.let {
            text.append(it)
            text.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val music = metadata.filter { it.first != CardMetadata.TITLE }.joinToString(" · ") { it.second }
        if (music.isNotBlank()) {
            if (text.isNotEmpty()) text.append("\n")
            val start = text.length
            text.append(music)
            text.setSpan(RelativeSizeSpan(32f / 44f), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return text
    }

    private fun emphasizedText(content: CardContent, selected: Set<Int>, emphasized: Set<Int>, accent: Int): CharSequence {
        val text = SpannableStringBuilder()
        var hasLine = false
        content.lines.forEachIndexed { index, line -> if (index in selected) {
            if (hasLine) text.append("\n")
            hasLine = true
            val start = text.length
            text.append(line.text)
            if (index in emphasized && line.text.isNotBlank()) {
                text.setSpan(StyleSpan(Typeface.BOLD), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                text.setSpan(object : CharacterStyle() {
                    override fun updateDrawState(paint: TextPaint) {
                        paint.setShadowLayer(8f, 0f, 0f, ColorUtils.setAlphaComponent(accent, 150))
                    }
                }, start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } }
        return text
    }

    private fun drawBrand(canvas: Canvas, bounds: RectF, style: CardStyle, color: Int) {
        val markWidth = bounds.height() * 26f / 24f
        val markHeight = markWidth * 24f / 26f
        val namePaint = textPaint(bounds.height() * .50f, color, faces.getValue(CardFont.INTER))
        val gap = if (style.showAppName) bounds.height() * .22f else 0f
        if (style.showAppName) {
            val available = bounds.width() - markWidth - gap
            namePaint.textSize *= minOf(1f, available / namePaint.measureText("singalong"))
        }
        val nameWidth = if (style.showAppName) namePaint.measureText("singalong") else 0f
        val x = if (style.logoPosition.left) bounds.left else bounds.right - markWidth - gap - nameWidth
        ContextCompat.getDrawable(context, R.drawable.ic_singalong_mark)?.mutate()?.let { icon ->
            icon.setTint(if (style.logoStyle == CardLogoStyle.COLORED) 0xffb49be5.toInt() else color)
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
        } else when (style.background) {
            CardBackground.GRADIENT -> {
                paint.shader = RadialGradient(w * .9f, h * .12f, w,
                    ColorUtils.blendARGB(base, accent, .18f), base, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w, h, paint)
            }
            CardBackground.HALO -> {
                paint.shader = RadialGradient(w * .5f, h * .45f, maxOf(w, h) * .7f,
                    ColorUtils.blendARGB(base, accent, .30f), base, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w, h, paint)
            }
            CardBackground.WASH -> {
                listOf(.05f to .15f, .95f to .85f).forEach { (x, y) ->
                    paint.shader = RadialGradient(w * x, h * y, maxOf(w, h) * .8f,
                        ColorUtils.setAlphaComponent(accent, 55), ColorUtils.setAlphaComponent(accent, 0), Shader.TileMode.CLAMP)
                    canvas.drawRect(0f, 0f, w, h, paint)
                }
            }
            CardBackground.DUSK -> {
                paint.shader = LinearGradient(0f, 0f, w, h,
                    intArrayOf(base, ColorUtils.blendARGB(base, accent, .24f), base), floatArrayOf(0f, .65f, 1f), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w, h, paint)
            }
            CardBackground.PAPER -> {
                // Stable fine grain, shared by preview and export; no animated noise or image allocation.
                paint.color = ColorUtils.setAlphaComponent(style.palette.ink, 12)
                val random = kotlin.random.Random(47)
                repeat(8000) { canvas.drawCircle(random.nextFloat() * w, random.nextFloat() * h, .65f, paint) }
            }
            else -> Unit
        }
    }

    private fun textPaint(size: Float, color: Int, face: Typeface) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color or (0xff shl 24)
        typeface = face
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
