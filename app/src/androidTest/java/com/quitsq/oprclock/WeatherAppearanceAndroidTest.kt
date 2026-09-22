package com.quitsq.oprclock

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class WeatherAppearanceAndroidTest {
    private fun time(hour: Int, minute: Int = 0) = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo")).apply {
        clear()
        set(2026, Calendar.SEPTEMBER, 22, hour, minute)
    }.timeInMillis

    private fun forecast(code: Int = 0, now: Long = time(12)) = Forecast(
        listOf(ForecastHour(now, 25.0, 0, code)), now,
        listOf(SolarDay(time(6), time(18)))
    )

    @Test fun followsSunriseSunsetAndWeather() {
        val noon = WeatherAppearance.resolve(time(12), forecast())
        assertEquals(DisplayTheme.LIGHT, noon.palette)
        assertEquals(DisplayTheme.AMBER, WeatherAppearance.resolve(time(6), forecast(now = time(6))).palette)
        assertEquals(DisplayTheme.AMBER, WeatherAppearance.resolve(time(18), forecast(now = time(18))).palette)
        val night = WeatherAppearance.resolve(time(22), forecast(now = time(22)))
        assertEquals(DisplayTheme.BLUE, night.palette)
        assertNotEquals(noon.top, night.top)
        val rain = WeatherAppearance.resolve(time(12), forecast(61))
        val snow = WeatherAppearance.resolve(time(12), forecast(71))
        val storm = WeatherAppearance.resolve(time(12), forecast(95))
        assertEquals(DisplayTheme.BLUE, rain.palette)
        assertEquals(DisplayTheme.LIGHT, snow.palette)
        assertNotEquals(noon.top, snow.top)
        assertNotEquals(rain.top, storm.top)
    }

    @Test fun missingAndStaleDataUseTimeFallbackWithoutStaleWeather() {
        assertEquals(DisplayTheme.LIGHT, WeatherAppearance.resolve(time(12), null).palette)
        assertEquals(DisplayTheme.BLUE, WeatherAppearance.resolve(time(23), null).palette)
        assertTrue(WeatherAppearance.resolve(time(12), null).label.contains("目安"))
        val stale = forecast(95).copy(fetchedAt = time(8))
        assertTrue(WeatherAppearance.resolve(time(12), stale).label.contains("天気待ち"))
        assertEquals(DisplayTheme.AUTO, DisplayTheme.fromName(null))
    }
}
