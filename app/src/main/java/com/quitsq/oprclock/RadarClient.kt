package com.quitsq.oprclock

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

data class RadarFrame(val bitmap: Bitmap, val observedAt: Long)

/** Latest observed JMA rainfall, centered on Nagoya. All work runs off the UI thread. */
class RadarClient {
    private val backgrounds = mutableMapOf<String, Bitmap>()

    fun fetch(): RadarFrame {
        val times = JSONArray(String(download("https://www.jma.go.jp/bosai/jmatile/data/nowc/targetTimes_N1.json"), Charsets.UTF_8))
        val latest = (0 until times.length()).map { times.getJSONObject(it) }
            .filter { it.getString("basetime") == it.getString("validtime") &&
                (0 until it.getJSONArray("elements").length()).any { i -> it.getJSONArray("elements").getString(i) == "hrpns" } }
            .maxByOrNull { it.getString("validtime") } ?: error("No radar observation available")
        val base = latest.getString("basetime")
        val valid = latest.getString("validtime")
        val observedAt = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }.parse(valid)?.time ?: error("Invalid radar observation time")
        val width = 536
        val height = 256
        val zoom = 8
        val world = 256.0 * (1 shl zoom)
        val latitude = Math.toRadians(35.1815)
        val left = (136.9066 + 180.0) / 360.0 * world - width / 2.0
        val top = (1.0 - ln(tan(latitude) + 1.0 / cos(latitude)) / PI) / 2.0 * world - height / 2.0
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            for (y in floor(top / 256).toInt()..floor((top + height - 1) / 256).toInt()) {
                for (x in floor(left / 256).toInt()..floor((left + width - 1) / 256).toInt()) {
                    if (Thread.currentThread().isInterrupted) throw InterruptedException()
                    val tile = "$zoom/$x/$y.png"
                    val background = backgrounds.getOrPut(tile) {
                        decode(download("https://www.jma.go.jp/tile/gsi/pale/$tile"))
                    }
                    val dx = (x * 256 - left).toFloat()
                    val dy = (y * 256 - top).toFloat()
                    canvas.drawBitmap(background, dx, dy, null)
                    val radar = decode(download("https://www.jma.go.jp/bosai/jmatile/data/nowc/$base/none/$valid/surf/hrpns/$tile"))
                    canvas.drawBitmap(radar, dx, dy, null)
                    radar.recycle()
                }
            }
            return RadarFrame(bitmap, observedAt)
        } catch (error: Exception) {
            bitmap.recycle()
            throw error
        }
    }

    private fun decode(bytes: ByteArray): Bitmap =
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("Invalid radar image")

    private fun download(url: String): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            check(connection.responseCode in 200..299) { "Radar HTTP ${connection.responseCode}" }
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }
}
