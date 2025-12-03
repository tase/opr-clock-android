package com.quitsq.oprclock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import kotlin.math.min
import java.util.*

class SignageClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val designWidth = 1920f
    private val designHeight = 540f

    private val bgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    private val labelBgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = 60f
        typeface = ResourcesCompat.getFont(context, R.font.kosugimaru_regular)
    }

    private val segOnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.RIGHT
        textSize = 110f
        typeface = ResourcesCompat.getFont(context, R.font.dseg7classic_bolditalic)
    }

    private val segOffPaint = Paint(segOnPaint).apply {
        color = Color.rgb(34, 34, 34)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        textSize = 60f
        typeface = ResourcesCompat.getFont(context, R.font.kosugimaru_regular)
    }

    private val weekdayMainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.RIGHT
        textSize = 100f
        typeface = ResourcesCompat.getFont(context, R.font.kosugimaru_regular)
    }

    private val weekdayRestPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        textSize = 60f
        typeface = ResourcesCompat.getFont(context, R.font.kosugimaru_regular)
    }

    private var jstCal: Calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"))
    private var utcCal: Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

    fun setTime(jst: Calendar, utc: Calendar) {
        jstCal = jst
        utcCal = utc
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val sx = width / designWidth
        val sy = height / designHeight
        val scale = min(sx, sy)

        canvas.save()
        canvas.scale(scale, scale)

        // 背景
        canvas.drawRect(0f, 0f, designWidth, designHeight, bgPaint)

        // 上段ラベル＆時刻（JST）
        drawLabel(canvas, designWidth / 2f, 50f, "日本標準時")
        drawLine(canvas, 230f, jstCal)

        // 下段ラベル＆時刻（UTC）
        val half = designHeight / 2f
        drawLabel(canvas, designWidth / 2f, half + 50f, "グリニッジ標準時")
        drawLine(canvas, designHeight - 40f, utcCal)

        canvas.restore()
    }

    private fun drawLabel(canvas: Canvas, cx: Float, centerY: Float, label: String) {
        val h = 80f
        val top = centerY - h / 2f -10
        val bottom = centerY + h / 2f
        canvas.drawRect(0f, top, designWidth, bottom, labelBgPaint)
        canvas.drawText(label, cx, centerY + h * 0.2f, labelTextPaint)
    }

    private fun draw7Seg(canvas: Canvas, x: Float, baseY: Float, base: String, text: String) {
        canvas.drawText(base, x, baseY, segOffPaint)
        canvas.drawText(text, x, baseY, segOnPaint)
    }

    private fun drawLine(canvas: Canvas, baseY: Float, cal: Calendar) {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)

        // 年
        draw7Seg(canvas, 355f, baseY, "8888", year.toString())
        canvas.drawText("年", 355f, baseY, textPaint)

        // 月
        draw7Seg(canvas, 565f, baseY, "18", month.toString())
        canvas.drawText("月", 565f, baseY, textPaint)

        // 日
        draw7Seg(canvas, 825f, baseY, "88", day.toString())
        canvas.drawText("日", 825f, baseY, textPaint)

        // 曜日
        val weekdays = arrayOf("月","火","水","木","金","土","日")
        val dow = cal.get(Calendar.DAY_OF_WEEK) // SUNDAY=1
        val idx = (dow + 5) % 7  // 月曜=0
        val wMain = weekdays[idx]
        canvas.drawText(wMain, 1010f, baseY, weekdayMainPaint)
        canvas.drawText("曜日", 1020f, baseY, weekdayRestPaint)

        // 時
        draw7Seg(canvas, 1340f, baseY, "88", hour.toString())
        canvas.drawText("時", 1340f, baseY, textPaint)

        // 分
        val minuteStr = String.format("%02d", minute)
        draw7Seg(canvas, 1600f, baseY, "88", minuteStr)
        canvas.drawText("分", 1600f, baseY, textPaint)

        // 秒
        val secondStr = String.format("%02d", second)
        draw7Seg(canvas, 1860f, baseY, "88", secondStr)
        canvas.drawText("秒", 1860f, baseY, textPaint)
    }
}
