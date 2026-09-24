package com.example.gymapplication

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A fully custom swipeable steps card.
 * Swipe LEFT  → Yesterday
 * Swipe RIGHT → Today
 * Tap anywhere → opens StepChartActivity via onGoalTap callback
 */
class StepsCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // ── Data ──────────────────────────────────────────────
    data class DayData(
        val steps: Int = 0,
        val goal: Int = 8000,
        val label: String = "Today",
        val calories: Int = 0,
        val activeMinutes: Int = 0
    ) {
        val progress: Float
            get() = if (goal > 0) (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
        val goalReached: Boolean
            get() = steps >= goal
    }

    var todayData: DayData = DayData()
        set(value) {
            field = value
            if (showingToday) animateProgress(value.progress)
            invalidate()
        }

    var yesterdayData: DayData = DayData(label = "Yesterday")
        set(value) {
            field = value
            if (!showingToday) animateProgress(value.progress)
            invalidate()
        }

    var onGoalTap: (() -> Unit)? = null

    // ── State ─────────────────────────────────────────────
    private var showingToday = true

    /**
     * swipeOffsetX meaning:
     *   0f       = showing Today   (default, no translation)
     *   +width   = showing Yesterday (canvas shifted right, yesterday comes into view from left)
     *
     * Layout:
     *   Yesterday page drawn at xOffset = -width
     *   Today     page drawn at xOffset = 0
     *   canvas.translate(swipeOffsetX, 0) → at swipeOffsetX=+width, yesterday is centered
     */
    private var swipeOffsetX = 0f
    private var animatedProgress = 0f
    private var progressAnimator: ValueAnimator? = null
    private var swipeAnimator: ValueAnimator? = null

    // ── Paints ────────────────────────────────────────────
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val arcFgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ── Arc rect ─────────────────────────────────────────
    private val arcRect = RectF()

    // ── Gesture detector (tap only) ───────────────────────
    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                onGoalTap?.invoke()
                return true
            }
        })

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isDragging = false
    private val DRAG_SLOP = 15f  // px before we decide horizontal drag

    // ── Public control ────────────────────────────────────
    fun refreshToday() {
        if (showingToday) animateProgress(todayData.progress)
    }

    private fun switchTo(today: Boolean) {
        showingToday = today
        // Today = offset 0, Yesterday = offset +width
        val targetOffset = if (today) 0f else width.toFloat()
        swipeAnimator?.cancel()
        swipeAnimator = ValueAnimator.ofFloat(swipeOffsetX, targetOffset).apply {
            duration = 280
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { swipeOffsetX = it.animatedValue as Float; invalidate() }
            start()
        }
        val data = if (today) todayData else yesterdayData
        animateProgress(data.progress)
    }

    private fun animateProgress(target: Float) {
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(animatedProgress, target).apply {
            duration = 900
            interpolator = OvershootInterpolator(0.8f)
            addUpdateListener { animatedProgress = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    // ── Touch ─────────────────────────────────────────────
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Always forward to gesture detector — handles tap detection
        gestureDetector.onTouchEvent(event)

        parent?.requestDisallowInterceptTouchEvent(true)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                isDragging = false
                swipeAnimator?.cancel()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - touchStartX
                val dy = event.y - touchStartY

                if (!isDragging) {
                    return when {
                        abs(dx) > DRAG_SLOP && abs(dx) > abs(dy) -> {
                            isDragging = true
                            true
                        }
                        abs(dy) > DRAG_SLOP -> {
                            // Vertical scroll wins — release parent interception
                            parent?.requestDisallowInterceptTouchEvent(false)
                            false
                        }
                        else -> true
                    }
                }

                // Drag offset is relative to the page we started on
                val baseOffset = if (showingToday) 0f else width.toFloat()
                // clamp between 0 (today fully visible) and +width (yesterday fully visible)
                swipeOffsetX = (baseOffset + (event.x - touchStartX))
                    .coerceIn(0f, width.toFloat())
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!isDragging) {
                    // Pure tap already handled by gestureDetector above
                    return true
                }
                isDragging = false
                val threshold = width * 0.25f  // 25% of width to commit page change

                when {
                    // Moving toward yesterday (offset growing from 0)
                    showingToday && swipeOffsetX > threshold -> switchTo(false)
                    // Moving toward today (offset shrinking from width)
                    !showingToday && swipeOffsetX < (width - threshold) -> switchTo(true)
                    // Not enough — snap back
                    else -> switchTo(showingToday)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    // ── Drawing ───────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.save()
        canvas.translate(swipeOffsetX, 0f)

        // Yesterday sits to the LEFT of today
        drawPage(canvas, yesterdayData, -w, w, h)
        drawPage(canvas, todayData, 0f, w, h)

        canvas.restore()

        // Dots are drawn in screen-space (not translated)
        drawDots(canvas, w, h)
    }

    private fun drawPage(canvas: Canvas, data: DayData, xOffset: Float, w: Float, h: Float) {
        val goalReached = data.goalReached

        // Background gradient
        val bgShader = LinearGradient(
            xOffset, 0f, xOffset + w, h,
            if (goalReached)
                intArrayOf(Color.parseColor("#1B5E20"), Color.parseColor("#2E7D32"))
            else
                intArrayOf(Color.parseColor("#0D0D23"), Color.parseColor("#1A1A2E")),
            null, Shader.TileMode.CLAMP
        )
        bgPaint.shader = bgShader
        canvas.drawRoundRect(xOffset, 0f, xOffset + w, h, dp(24f), dp(24f), bgPaint)

        val cx = xOffset + w / 2f
        val topY = dp(32f)

        // ── Day label ─────────────────────────────────────
        labelPaint.textSize = sp(13f)
        labelPaint.color = if (goalReached) Color.parseColor("#A5D6A7") else Color.parseColor("#8899BB")
        canvas.drawText(data.label.uppercase(), cx, topY, labelPaint)

        // ── Big arc ──────────────────────────────────────
        val arcStroke = dp(14f)
        val arcRadius = min(w, h) * 0.26f
        val arcLeft   = cx - arcRadius
        val arcTop    = topY + dp(24f)
        val arcRight  = cx + arcRadius
        val arcBottom = arcTop + arcRadius * 2f

        arcRect.set(arcLeft, arcTop, arcRight, arcBottom)

        arcBgPaint.strokeWidth = arcStroke
        arcBgPaint.color = if (goalReached) Color.parseColor("#1B5E20") else Color.parseColor("#1E2040")
        canvas.drawArc(arcRect, 135f, 270f, false, arcBgPaint)

        arcFgPaint.strokeWidth = arcStroke
        val arcColor = when {
            goalReached              -> Color.parseColor("#69F0AE")
            animatedProgress > 0.75f -> Color.parseColor("#FFB74D")
            animatedProgress > 0.5f  -> Color.parseColor("#5B8DEF")
            else                     -> Color.parseColor("#3D5AFE")
        }
        arcFgPaint.shader = SweepGradient(
            cx, arcTop + arcRadius,
            intArrayOf(arcColor, lighten(arcColor, 0.3f), arcColor),
            floatArrayOf(0f, 0.5f, 1f)
        )
        val sweep = 270f * animatedProgress
        canvas.drawArc(arcRect, 135f, sweep, false, arcFgPaint)
        arcFgPaint.shader = null

        // ── Steps count ──────────────────────────────────
        val arcCenterY = arcTop + arcRadius
        textPaint.textSize = sp(36f)
        textPaint.color = Color.WHITE
        canvas.drawText(
            if (data.steps > 0) formatSteps(data.steps) else "—",
            cx, arcCenterY + sp(14f), textPaint
        )

        subTextPaint.textSize = sp(11f)
        subTextPaint.color = if (goalReached) Color.parseColor("#81C784") else Color.parseColor("#5B8DEF")
        canvas.drawText("steps", cx, arcCenterY + sp(14f) + dp(20f), subTextPaint)

        // ── Status label ─────────────────────────────────
        val pct = (data.progress * 100).roundToInt()
        labelPaint.textSize = sp(13f)
        labelPaint.color = if (goalReached) Color.parseColor("#A5D6A7") else Color.parseColor("#8899BB")
        val statusText = when {
            goalReached     -> "🎉 Goal Reached!"
            data.steps == 0 -> "Tap to view chart"
            else            -> "$pct% of ${formatSteps(data.goal)} goal"
        }
        canvas.drawText(statusText, cx, arcBottom + dp(12f), labelPaint)

        // ── 3-col stats row ──────────────────────────────
        val statsY = arcBottom + dp(48f)
        val col1 = xOffset + w * 0.2f
        val col2 = xOffset + w * 0.5f
        val col3 = xOffset + w * 0.8f

        drawStat(canvas, col1, statsY, "🔥", "${data.calories}", "kcal", goalReached)
        drawStat(canvas, col2, statsY, "⚡", "${data.activeMinutes}", "active min", goalReached)
        drawStat(canvas, col3, statsY, "🎯", "${data.goal}", "daily goal", goalReached)

        // ── Swipe hint ────────────────────────────────────
        val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sp(10f)
            textAlign = Paint.Align.CENTER
            color = if (goalReached) Color.parseColor("#A5D6A7") else Color.parseColor("#444466")
        }
        val hintY = h - dp(14f)
        val hint = if (data.label == "Today") "← swipe for yesterday" else "swipe for today →"
        canvas.drawText(hint, cx, hintY, hintPaint)
    }

    private fun drawStat(
        canvas: Canvas, cx: Float, y: Float,
        icon: String, value: String, label: String,
        goalReached: Boolean
    ) {
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sp(16f)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(icon, cx, y, emojiPaint)

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sp(15f)
            textAlign = Paint.Align.CENTER
            color = Color.WHITE
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        canvas.drawText(value, cx, y + dp(22f), valPaint)

        val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sp(10f)
            textAlign = Paint.Align.CENTER
            color = if (goalReached) Color.parseColor("#81C784") else Color.parseColor("#6677AA")
        }
        canvas.drawText(label, cx, y + dp(36f), lblPaint)
    }

    private fun drawDots(canvas: Canvas, w: Float, h: Float) {
        val dotRadius = dp(4f)
        val gap = dp(12f)
        val totalWidth = dotRadius * 4 + gap
        val startX = w / 2f - totalWidth / 2f
        val dotY = h - dp(28f)

        // Left dot = Today (white when active)
        dotPaint.color = if (showingToday) Color.WHITE else Color.parseColor("#444466")
        canvas.drawCircle(startX, dotY, dotRadius, dotPaint)

        // Right dot = Yesterday (green when active)
        dotPaint.color = if (!showingToday) Color.parseColor("#69F0AE") else Color.parseColor("#444466")
        canvas.drawCircle(startX + dotRadius * 2 + gap, dotY, dotRadius, dotPaint)
    }

    private fun formatSteps(n: Int): String = n.toString()

    private fun lighten(color: Int, factor: Float): Int {
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
    private fun sp(value: Float) = value * resources.displayMetrics.scaledDensity

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post { animateProgress(if (showingToday) todayData.progress else yesterdayData.progress) }
    }
}