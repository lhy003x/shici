package com.shici.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.shici.app.R
import kotlin.math.min

/**
 * 手写进度环：答辩演示时比 ProgressBar 更有辨识度。
 * 颜色随完成度从绿到琥珀过渡，呼应「记忆衰减」的概念。
 */
class RingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(10f)
        color = context.getColor(R.color.line)
        strokeCap = Paint.Cap.ROUND
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(10f)
        color = context.getColor(R.color.green)
        strokeCap = Paint.Cap.ROUND
    }

    private val mainText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink)
        textAlign = Paint.Align.CENTER
        textSize = dp(26f)
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
    }

    private val subText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink2)
        textAlign = Paint.Align.CENTER
        textSize = dp(11f)
    }

    private val rect = RectF()
    private var text = "0/0"
    private var sub = "今日进度"

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    fun setTexts(main: String, sub: String) {
        text = main
        this.sub = sub
        invalidate()
    }

    fun setColor(color: Int) {
        arcPaint.color = color
        invalidate()
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) / 2f - trackPaint.strokeWidth / 2f - dp(2f)
        rect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        canvas.drawArc(rect, 0f, 360f, false, trackPaint)
        if (progress > 0f) {
            canvas.drawArc(rect, -90f, 360f * progress, false, arcPaint)
        }

        val fm = mainText.fontMetrics
        val textY = cy - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, cx, textY, mainText)
        canvas.drawText(sub, cx, textY + fm.descent + dp(14f), subText)
    }

    companion object {
        @Suppress("unused")
        val DEFAULT_COLOR = Color.TRANSPARENT
    }
}
