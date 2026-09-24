package com.example.gymapplication

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable

/**
 * A Drawable that draws a large emoji centred on a rounded coloured background.
 * Used as the final fallback when all GIF loading attempts fail.
 */
class EmojiPlaceholderDrawable(
    private val context: Context,
    private val emoji: String,
    private val bgHexColor: String
) : Drawable() {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(bgHexColor)
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 80f * context.resources.displayMetrics.density
    }

    private val cornerRadius = 16f * context.resources.displayMetrics.density

    override fun draw(canvas: Canvas) {
        val b = bounds
        val rect = RectF(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        // Draw emoji centred
        val fm = textPaint.fontMetrics
        val textHeight = fm.descent - fm.ascent
        val x = (b.left + b.right) / 2f
        val y = (b.top + b.bottom) / 2f - (fm.descent + fm.ascent) / 2f

        canvas.drawText(emoji, x, y, textPaint)

        // Draw exercise label at bottom
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 14f * context.resources.displayMetrics.density
            alpha = 180
        }
        canvas.drawText("Tap card for full exercise details", x, b.bottom - 20f * context.resources.displayMetrics.density, labelPaint)
    }

    override fun setAlpha(alpha: Int) { bgPaint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { bgPaint.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}