package com.quitsq.oprclock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.LinearGradient
import android.graphics.Shader
import java.text.SimpleDateFormat
import android.util.AttributeSet
import android.text.TextPaint
import android.text.StaticLayout
import android.text.Layout
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
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

    private val dividerPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
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

    private val headlinePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 38f
        typeface = ResourcesCompat.getFont(context, R.font.kosugimaru_regular)
    }
    private var headlines = emptyList<String>()
    private var newsLayout: StaticLayout? = null
    private var scrollOffset = 0f
    private var previousFrame = 0L
    private var scrolling = false
    private val newsTop = 180f
    private val newsHeight = 310f
    private var newsUpdatedLabel = "ニュース最終取得：未取得"
    private val newsUpdatedFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).apply {
        timeZone = TimeZone.getTimeZone("Asia/Tokyo")
    }
    private val newsUpdatedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 18f
        typeface = headlinePaint.typeface
    }

    fun setHeadline(text: String) = setHeadlines(listOf(text))

    fun setHeadlines(items: List<String>, fetchedAt: Long? = null) {
        if (fetchedAt != null) {
            newsUpdatedLabel = "ニュース最終取得：${newsUpdatedFormat.format(Date(fetchedAt))} JST"
            invalidate()
        }
        if (headlines == items) return
        headlines = items.toList()
        contentDescription = items.joinToString("。") + "。長押しで表示テーマを変更"
        val text = items.joinToString("\n") { "・$it" }
        newsLayout = StaticLayout.Builder.obtain(
            text, 0, text.length,
            headlinePaint, 736
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setLineSpacing(18f, 1f)
            .build()
        scrollOffset = 0f
        previousFrame = 0L
        invalidate()
    }

    fun setScrolling(active: Boolean) {
        scrolling = active
        previousFrame = 0L
        invalidate()
    }

    private var jstCal: Calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"))

    fun setTime(jst: Calendar) {
        jstCal = jst
        refreshAppearance()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val sx = width / designWidth
        val sy = height / designHeight
        val scale = min(sx, sy)

        if (appearance != null) {
            val save = canvas.save()
            canvas.scale(width / designWidth, height / designHeight)
            canvas.drawRect(0f, 0f, designWidth, designHeight, bgPaint)
            canvas.restoreToCount(save)
        } else canvas.drawColor(activePalette.background)
        canvas.save()
        canvas.scale(scale, scale)

        // 背景
        canvas.drawRect(0f, 0f, designWidth, designHeight, bgPaint)

        drawLine(canvas, 140f, jstCal)
        canvas.drawRect(24f, 161f, designWidth - 24f, 163f, dividerPaint)
        drawNews(canvas)
        drawRadar(canvas)
        drawForecast(canvas)
        canvas.restore()
    }

    private var showForecastGraph = true
    private var trackingForecastSwipe = false
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private val swipeThreshold = maxOf(
        ViewConfiguration.get(context).scaledTouchSlop * 2f,
        32f * resources.displayMetrics.density
    )

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val scale = min(width / designWidth, height / designHeight)
                if (scale <= 0f || event.x / scale !in 1360f..1896f ||
                    event.y / scale !in 175f..530f) return handled
                swipeStartX = event.x
                swipeStartY = event.y
                trackingForecastSwipe = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!trackingForecastSwipe) return handled
                val dx = kotlin.math.abs(event.x - swipeStartX)
                val dy = kotlin.math.abs(event.y - swipeStartY)
                if (dy > swipeThreshold && dy > dx) trackingForecastSwipe = false
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!trackingForecastSwipe) return handled
                trackingForecastSwipe = false
                val dx = kotlin.math.abs(event.x - swipeStartX)
                val dy = kotlin.math.abs(event.y - swipeStartY)
                if (dx >= swipeThreshold && dx > dy * 1.5f) {
                    showForecastGraph = !showForecastGraph
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
                val wasTracking = trackingForecastSwipe
                trackingForecastSwipe = false
                return wasTracking
            }
        }
        return trackingForecastSwipe || handled
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private var radar: RadarFrame? = null
    private var radarFailed = false
    private val radarPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val radarTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        typeface = headlinePaint.typeface
    }
    private val radarTimeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN).apply {
        timeZone = TimeZone.getTimeZone("Asia/Tokyo")
    }

    fun setRadar(frame: RadarFrame) {
        radar = frame
        radarFailed = false
        invalidate()
    }

    fun setRadarFailed() {
        radarFailed = true
        invalidate()
    }

    private fun drawRadar(canvas: Canvas) {
        val frame = radar
        val time = frame?.let { radarTimeFormat.format(Date(it.observedAt)) + " JST" } ?: ""
        canvas.drawText("名古屋 雨雲レーダー  $time", 800f, 197f, radarTextPaint)
        if (frame != null) {
            canvas.drawBitmap(frame.bitmap, null, RectF(800f, 209f, 1336f, 465f), radarPaint)
            radarPaint.color = Color.BLACK
            canvas.drawCircle(1068f, 337f, 6f, radarPaint)
            radarPaint.color = Color.WHITE
            canvas.drawCircle(1068f, 337f, 3f, radarPaint)
        } else {
            canvas.drawText(if (radarFailed) "取得失敗・自動再試行します" else "レーダー画像を取得しています…",
                800f, 335f, radarTextPaint)
        }
        val stale = frame != null && System.currentTimeMillis() - frame.observedAt > 20 * 60_000L
        radarTextPaint.textSize = 18f
        canvas.drawText(if (radarFailed || stale) "更新待ち：最新画像を取得できていません" else "出典：気象庁 ／ 地図：地理院タイル", 800f, 490f, radarTextPaint)
        canvas.drawText(if (radarFailed || stale) "出典：気象庁 ／ 地図：地理院タイル" else "雨の強さ：青 → 黄 → 赤 → 紫（強）", 800f, 517f, radarTextPaint)
        radarTextPaint.textSize = 22f
    }

    private var forecast: Forecast? = null
    private var forecastFailed = false
    private val forecastPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 18f
        typeface = headlinePaint.typeface
    }
    private val forecastTime = SimpleDateFormat("dd日HH時", Locale.JAPAN).apply {
        timeZone = TimeZone.getTimeZone("Asia/Tokyo")
    }

    private val forecastEndTime = SimpleDateFormat("HH時", Locale.JAPAN).apply {
        timeZone = TimeZone.getTimeZone("Asia/Tokyo")
    }

    private val temperatureGraphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 193, 92)
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }
    private val rainGraphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(91, 180, 255)
    }
    private val graphGridPaint = Paint().apply {
        color = Color.rgb(65, 65, 65)
        strokeWidth = 1f
    }

    var displayTheme: DisplayTheme = DisplayTheme.STANDARD
        private set
    private var activePalette = DisplayTheme.STANDARD
    private var appearance: WeatherAppearance? = null
    var onThemeRequested: (() -> Unit)? = null

    init {
        setOnLongClickListener {
            trackingForecastSwipe = false
            onThemeRequested?.invoke()
            true
        }
        setDisplayTheme(DisplayTheme.STANDARD)
    }

    fun setDisplayTheme(theme: DisplayTheme) {
        displayTheme = theme
        refreshAppearance()
    }

    private fun refreshAppearance() {
        val next = if (displayTheme == DisplayTheme.AUTO)
            WeatherAppearance.resolve(jstCal.timeInMillis, forecast) else null
        val theme = next?.palette ?: displayTheme
        if (appearance == next && activePalette == theme) return
        appearance = next
        activePalette = theme
        bgPaint.shader = next?.let {
            LinearGradient(0f, 0f, 0f, designHeight, it.top, it.bottom, Shader.TileMode.CLAMP)
        }
        bgPaint.color = theme.background
        listOf(dividerPaint, segOnPaint, textPaint, weekdayMainPaint, weekdayRestPaint,
            headlinePaint, radarTextPaint, forecastPaint).forEach { it.color = theme.foreground }
        segOffPaint.color = theme.inactiveSegment
        newsUpdatedPaint.color = theme.secondary
        temperatureGraphPaint.color = theme.temperature
        rainGraphPaint.color = theme.rain
        graphGridPaint.color = theme.grid
        invalidate()
    }

    fun setForecast(value: Forecast) {
        forecast = value
        refreshAppearance()
        forecastFailed = false
        invalidate()
    }

    fun setForecastFailed() {
        forecastFailed = true
        invalidate()
    }

    private fun drawForecast(canvas: Canvas) {
        forecastPaint.textSize = 22f
        canvas.drawText("名古屋 24時間天気予報（JST）", 1360f, 197f, forecastPaint)
        forecastPaint.textSize = 17f
        forecastPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(if (showForecastGraph) "詳細 ↔［グラフ］" else "［詳細］↔ グラフ",
            1896f, 197f, forecastPaint)
        forecastPaint.textAlign = Paint.Align.LEFT
        val data = forecast
        if (data == null) {
            canvas.drawText(if (forecastFailed) "取得失敗・自動再試行します" else "天気予報を取得しています…", 1360f, 335f, forecastPaint)
        } else {
            if (showForecastGraph) drawForecastGraph(canvas, data) else drawForecastDetails(canvas, data)
        }
        val stale = data != null && (System.currentTimeMillis() - data.fetchedAt > 60 * 60_000L || data.hours.first().time < System.currentTimeMillis())
        val status = if (forecastFailed || stale) "更新待ち" else data?.let { "取得 " + radarTimeFormat.format(Date(it.fetchedAt)) } ?: ""
        forecastPaint.textSize = 17f
        canvas.drawText("Open-Meteo (CC BY 4.0)  $status", 1360f, 520f, forecastPaint)
    }

    private fun drawForecastDetails(canvas: Canvas, data: Forecast) {
        forecastPaint.textSize = 17f
        for (column in 0..1) {
            val x = 1360f + column * 276f
            canvas.drawText("時刻", x, 230f, forecastPaint)
            canvas.drawText("天気", x + 90f, 230f, forecastPaint)
            canvas.drawText("気温", x + 150f, 230f, forecastPaint)
            canvas.drawText("降水%", x + 212f, 230f, forecastPaint)
            data.hours.drop(column * 12).take(12).forEachIndexed { index, hour ->
                val y = 254f + index * 22f
                canvas.drawText(forecastTime.format(Date(hour.time)), x, y, forecastPaint)
                canvas.drawText(ForecastClient.description(hour.weatherCode), x + 90f, y, forecastPaint)
                val temperature = hour.temperature?.takeIf { it.isFinite() }
                    ?.let { String.format(Locale.JAPAN, "%.0f°", it) } ?: "—"
                canvas.drawText(temperature, x + 150f, y, forecastPaint)
                canvas.drawText(hour.rainProbability?.let { "$it%" } ?: "—", x + 212f, y, forecastPaint)
            }
        }
    }

    private fun drawForecastGraph(canvas: Canvas, data: Forecast) {
        val hours = data.hours
        if (hours.isEmpty()) return
        val left = 1410f
        val right = 1880f
        val step = (right - left) / hours.size
        fun x(index: Int) = left + (index + 0.5f) * step

        forecastPaint.textSize = 17f
        canvas.drawText(
            forecastTime.format(Date(hours.first().time)) + " 〜 " +
                forecastTime.format(Date(hours.last().time)), 1360f, 227f, forecastPaint)
        forecastPaint.color = temperatureGraphPaint.color
        canvas.drawText("気温 ℃", 1360f, 253f, forecastPaint)
        val temperatures = hours.mapNotNull { it.temperature?.takeIf { value -> value.isFinite() } }
        val low = kotlin.math.floor((temperatures.minOrNull() ?: 0.0) / 5) * 5
        val high = maxOf(low + 5, kotlin.math.ceil((temperatures.maxOrNull() ?: 5.0) / 5) * 5)
        fun temperatureY(value: Double) = 342f - ((value - low) / (high - low) * 74f).toFloat()
        forecastPaint.textAlign = Paint.Align.RIGHT
        for (value in listOf(low, high)) {
            val y = temperatureY(value)
            canvas.drawLine(left, y, right, y, graphGridPaint)
            canvas.drawText(String.format(Locale.JAPAN, "%.0f°", value), left - 8f, y + 5f, forecastPaint)
        }
        hours.forEachIndexed { index, hour ->
            val temperature = hour.temperature?.takeIf { it.isFinite() } ?: return@forEachIndexed
            val y = temperatureY(temperature)
            val previous = hours.getOrNull(index - 1)?.temperature?.takeIf { it.isFinite() }
            if (previous != null) {
                canvas.drawLine(x(index - 1), temperatureY(previous), x(index), y, temperatureGraphPaint)
            }
            canvas.drawCircle(x(index), y, 2.5f, temperatureGraphPaint)
        }
        forecastPaint.textAlign = Paint.Align.LEFT
        if (temperatures.isEmpty()) canvas.drawText("データなし", left, 310f, forecastPaint)

        forecastPaint.color = rainGraphPaint.color
        canvas.drawText("降水確率 %", 1360f, 370f, forecastPaint)
        forecastPaint.textAlign = Paint.Align.RIGHT
        for (probability in listOf(0, 100)) {
            val y = 436f - probability * 0.5f
            canvas.drawLine(left, y, right, y, graphGridPaint)
            canvas.drawText("$probability", left - 8f, y + 5f, forecastPaint)
        }
        forecastPaint.textAlign = Paint.Align.CENTER
        hours.forEachIndexed { index, hour ->
            val probability = hour.rainProbability
            if (probability == null) {
                canvas.drawText("—", x(index), 425f, forecastPaint)
            } else {
                val top = 436f - probability.coerceIn(0, 100) * 0.5f
                canvas.drawRect(x(index) - step * 0.35f, top, x(index) + step * 0.35f, 436f, rainGraphPaint)
            }
        }
        forecastPaint.color = activePalette.foreground
        hours.chunked(3).forEachIndexed { index, group ->
            val center = x(index * 3 + (group.size - 1) / 2)
            canvas.drawText(forecastEndTime.format(Date(group[(group.size - 1) / 2].time)), center, 461f, forecastPaint)
            val weather = group.groupingBy { ForecastClient.description(it.weatherCode) }
                .eachCount().maxByOrNull { it.value }?.key ?: "不明"
            canvas.drawText(weather, center, 486f, forecastPaint)
        }
        forecastPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawNews(canvas: Canvas) {
        canvas.drawText(newsUpdatedLabel, 24f, 520f, newsUpdatedPaint)
        newsUpdatedPaint.textAlign = Paint.Align.RIGHT
        newsUpdatedPaint.textSize = if (appearance != null) 12f else 18f
        canvas.drawText(appearance?.let { "自動：${it.label} ／ 長押しで変更" }
            ?: "長押しでテーマ変更", 760f, 520f, newsUpdatedPaint)
        newsUpdatedPaint.textSize = 18f
        newsUpdatedPaint.textAlign = Paint.Align.LEFT
        val layout = newsLayout ?: return
        val overflow = layout.height > newsHeight
        val now = SystemClock.uptimeMillis()
        val cycleHeight = layout.height + 18f
        if (scrolling && overflow && previousFrame != 0L) {
            scrollOffset = (scrollOffset + (now - previousFrame).coerceAtMost(100L) * 0.020f) % cycleHeight
        }
        previousFrame = now
        canvas.save()
        canvas.clipRect(24f, newsTop, 760f, newsTop + newsHeight)
        canvas.translate(24f, newsTop - scrollOffset)
        layout.draw(canvas)
        if (overflow) {
            canvas.translate(0f, cycleHeight)
            layout.draw(canvas)
        }
        canvas.restore()
        if (scrolling && overflow) postInvalidateOnAnimation()
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
