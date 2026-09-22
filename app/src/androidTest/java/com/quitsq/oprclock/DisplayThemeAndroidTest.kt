package com.quitsq.oprclock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayThemeAndroidTest {
    @Test fun selectionSurvivesActivityRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("display_settings", Context.MODE_PRIVATE)
        val original = preferences.getString("theme", null)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    activity.findViewById<SignageClockView>(R.id.signage_clock).performLongClick()
                }
                onView(withText("ライト")).perform(click())
                assertEquals("LIGHT", preferences.getString("theme", null))
                scenario.recreate()
                scenario.onActivity { activity ->
                    assertEquals(DisplayTheme.LIGHT,
                        activity.findViewById<SignageClockView>(R.id.signage_clock).displayTheme)
                }
            }
        } finally {
            preferences.edit().putString("theme", original).commit()
        }
    }

    @Test fun palettesCoverBackgroundAndSurviveGraphRedraw() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val view = SignageClockView(instrumentation.targetContext)
            view.layout(0, 0, 1000, 270)
            view.setHeadline("テーマ表示テスト")
            view.setForecast(Forecast((0 until 24).map {
                ForecastHour(1_800_000_000_000L + it * 3_600_000L, 20.0 + it % 6, it * 4, 0)
            }, System.currentTimeMillis()))
            for (theme in DisplayTheme.entries.filter { it != DisplayTheme.AUTO }) {
                view.setDisplayTheme(theme)
                val first = Bitmap.createBitmap(1000, 270, Bitmap.Config.ARGB_8888)
                val second = Bitmap.createBitmap(1000, 270, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(first))
                view.draw(Canvas(second))
                assertEquals(theme.background, first.getPixel(999, 269))
                assertTrue("Graph drawing must preserve ${theme.name} colors", first.sameAs(second))
                first.recycle()
                second.recycle()
            }
            assertEquals(DisplayTheme.STANDARD, DisplayTheme.fromName("unknown"))
        }
    }
}
