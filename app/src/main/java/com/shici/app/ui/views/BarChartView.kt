package com.shici.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.shici.app.R
import kotlin.math.max

/** 近 7 天复习量的迷你柱状图 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink2)
        textSize = dp(10f)
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink)
        textSize = dp(10f)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val rect = RectF()

    private var labels: List<String> = emptyList()
    private var values: List<Int> = emptyList()

    fun setData(labels: List<String>, values: List<Int>) {
        this.labels = labels
        this.values = values
        invalidate()
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return

        val bottom = height - dp(20f)
        val top = dp(16f)
        val slot = width.toFloat() / values.size
        val barWidth = slot * 0.42f
        val maxValue = max(1, values.maxOrNull() ?: 1)

        values.forEachIndexed { index, value ->
            val cx = slot * index + slot / 2f
            val h = (bottom - top) * (value.toFloat() / maxValue).coerceIn(0f, 1f)
            val left = cx - barWidth / 2f
            val right = cx + barWidth / 2f
            val barTop = bottom - max(h, dp(4f))

            barPaint.shader = LinearGradient(
                left, barTop, left, bottom,
                context.getColor(R.color.green),
                context.getColor(R.color.green_soft),
                Shader.TileMode.CLAMP,
            )
            rect.set(left, barTop, right, bottom)
            canvas.drawRoundRect(rect, barWidth / 2f, barWidth / 2f, barPaint)

            if (value > 0) {
                canvas.drawText(value.toString(), cx, barTop - dp(5f), valuePaint)
            }
            canvas.drawText(labels.getOrElse(index) { "" }, cx, height - dp(4f), labelPaint)
        }
    }
}
