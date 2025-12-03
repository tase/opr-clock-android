package com.quitsq.oprclock

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ForecastHour(val time: Long, val temperature: Double?, val rainProbability: Int?, val weatherCode: Int?)
data class Forecast(val hours: List<ForecastHour>, val fetchedAt: Long)

class ForecastClient {
    fun fetch(): Forecast {
        val connection = URL("https://api.open-meteo.com/v1/forecast?latitude=35.1815&longitude=136.9066&hourly=temperature_2m,precipitation_probability,weather_code&forecast_days=3&timeformat=unixtime&timezone=Asia%2FTokyo").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            check(connection.responseCode in 200..299) { "Forecast HTTP ${connection.responseCode}" }
            return connection.inputStream.bufferedReader().use { parse(it.readText(), System.currentTimeMillis()) }
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(json: String, now: Long): Forecast {
        val hourly = JSONObject(json).getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val temperatures = hourly.getJSONArray("temperature_2m")
        val probabilities = hourly.getJSONArray("precipitation_probability")
        val codes = hourly.getJSONArray("weather_code")
        check(listOf(temperatures, probabilities, codes).all { it.length() == times.length() })
        val hours = (0 until times.length()).map { i ->
            ForecastHour(times.getLong(i) * 1000,
                if (temperatures.isNull(i)) null else temperatures.getDouble(i),
                if (probabilities.isNull(i)) null else probabilities.getInt(i),
                if (codes.isNull(i)) null else codes.getInt(i))
        }.filter { it.time >= now }.take(24)
        check(hours.size == 24 && hours.zipWithNext().all { (a, b) -> b.time - a.time == 3_600_000L }) {
            "Incomplete 24-hour forecast"
        }
        return Forecast(hours, now)
    }

    companion object {
        fun description(code: Int?): String = when (code) {
            0, 1 -> "晴れ"
            2 -> "晴曇"
            3 -> "曇り"
            45, 48 -> "霧"
            51, 53, 55, 56, 57 -> "霧雨"
            61, 63, 65, 66, 67, 80, 81, 82 -> "雨"
            71, 73, 75, 77, 85, 86 -> "雪"
            95, 96, 99 -> "雷雨"
            else -> "不明"
        }
    }
}
