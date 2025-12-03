package com.quitsq.oprclock

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ForecastClientAndroidTest {
    private fun fixture(count: Int = 48): String {
        val hourly = JSONObject()
        hourly.put("time", JSONArray((0 until count).map { 1_800_000_000L + it * 3600L }))
        hourly.put("temperature_2m", JSONArray((0 until count).map { if (it == 1) JSONObject.NULL else 25 }))
        hourly.put("precipitation_probability", JSONArray((0 until count).map { 60 }))
        hourly.put("weather_code", JSONArray((0 until count).map { 61 }))
        return JSONObject().put("hourly", hourly).toString()
    }

    @Test fun selectsNext24HoursAndPreservesMissingValues() {
        val result = ForecastClient().parse(fixture(), 1_800_000_001_000L)
        assertEquals(24, result.hours.size)
        assertEquals(1_800_003_600_000L, result.hours.first().time)
        assertNull(result.hours.first().temperature)
        assertEquals(60, result.hours.first().rainProbability)
        assertEquals("雨", ForecastClient.description(result.hours.first().weatherCode))
        assertEquals("不明", ForecastClient.description(null))
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsIncompleteForecast() {
        ForecastClient().parse(fixture(12), 1_800_000_001_000L)
    }
}
