package com.quitsq.oprclock

/** Hourly weather forecasts and sunrise/sunset for the same Nagoya location as the forecast. */
data class WeatherAppearance(val palette: DisplayTheme, val top: Int, val bottom: Int, val label: String) {
    companion object {
        fun resolve(now: Long, forecast: Forecast?): WeatherAppearance {
            val day = (now + 9 * 3_600_000L) / 86_400_000L
            val solar = forecast?.solarDays?.firstOrNull {
                (it.sunrise + 9 * 3_600_000L) / 86_400_000L == day
            }
            val hour = ((now + 9 * 3_600_000L) % 86_400_000L) / 3_600_000L
            val daylight = solar?.let { now >= it.sunrise && now < it.sunset } ?: (hour in 6..17)
            val twilight = solar != null && (kotlin.math.abs(now - solar.sunrise) < 30 * 60_000L ||
                kotlin.math.abs(now - solar.sunset) < 30 * 60_000L)
            val code = forecast?.takeIf { now - it.fetchedAt in 0..3 * 3_600_000L }
                ?.ambientHours?.lastOrNull { now - it.time in 0 until 3_600_000L }?.weatherCode
            val weather = when (code) {
                0, 1 -> "晴れ"
                2, 3, 45, 48 -> "曇り"
                51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "雨"
                71, 73, 75, 77, 85, 86 -> "雪"
                95, 96, 99 -> "雷雨"
                else -> "天気待ち"
            }
            val period = if (twilight) "朝夕" else if (daylight) "昼" else "夜"
            val label = "$period・$weather" + if (solar == null) "（時刻目安）" else ""
            if (!daylight && !twilight) return WeatherAppearance(DisplayTheme.BLUE,
                when (weather) {
                    "雷雨" -> 0xFF201329.toInt()
                    "雨" -> 0xFF142F40.toInt()
                    "雪" -> 0xFF283747.toInt()
                    "曇り" -> 0xFF242D39.toInt()
                    else -> 0xFF101D32.toInt()
                },
                0xFF030810.toInt(), label)
            return when (weather) {
                "雷雨" -> WeatherAppearance(DisplayTheme.BLUE, 0xFF2D233D.toInt(), 0xFF111521.toInt(), label)
                "雨" -> WeatherAppearance(DisplayTheme.BLUE, 0xFF263D52.toInt(), 0xFF0C1929.toInt(), label)
                "雪" -> WeatherAppearance(DisplayTheme.LIGHT, 0xFFDBE8F3.toInt(), 0xFFF5F7FA.toInt(), label)
                "曇り" -> WeatherAppearance(DisplayTheme.LIGHT, 0xFFC1CCD6.toInt(), 0xFFE5EAF0.toInt(), label)
                else -> if (twilight) WeatherAppearance(DisplayTheme.AMBER, 0xFF483024.toInt(), 0xFF171108.toInt(), label)
                    else WeatherAppearance(DisplayTheme.LIGHT, 0xFFBBDCF6.toInt(), 0xFFFFF0CF.toInt(), label)
            }
        }
    }
}
