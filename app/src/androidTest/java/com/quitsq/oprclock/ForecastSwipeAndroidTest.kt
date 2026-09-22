package com.quitsq.oprclock

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastSwipeAndroidTest {
    @Test fun forecastSwipesRespectBoundsDirectionAndScaledLayout() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val view = SignageClockView(instrumentation.targetContext)
            view.layout(0, 0, 960, 270)
            view.setForecast(Forecast((0 until 24).map {
                ForecastHour(1_800_000_000_000L + it * 3_600_000L, 20.0 + it % 6, it * 4, 0)
            }, System.currentTimeMillis()))
            fun render(): Bitmap = Bitmap.createBitmap(960, 270, Bitmap.Config.ARGB_8888).also {
                view.draw(Canvas(it))
            }
            fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, cancel: Boolean = false) {
                listOf(
                    Triple(MotionEvent.ACTION_DOWN, startX, startY),
                    Triple(MotionEvent.ACTION_MOVE, endX, endY),
                    Triple(if (cancel) MotionEvent.ACTION_CANCEL else MotionEvent.ACTION_UP, endX, endY)
                ).forEachIndexed { index, (action, x, y) ->
                    val event = MotionEvent.obtain(0, index * 100L, action, x, y, 0)
                    view.onTouchEvent(event)
                    event.recycle()
                }
            }
            val graph = render()
            swipe(920f, 170f, 710f, 170f)
            val detail = render()
            assertFalse("Horizontal swipe shows details", graph.sameAs(detail))
            swipe(710f, 170f, 920f, 170f)
            assertTrue("Reverse swipe returns to graph", graph.sameAs(render()))
            swipe(500f, 170f, 300f, 170f)
            assertTrue("Outside forecast does not toggle", graph.sameAs(render()))
            swipe(800f, 120f, 800f, 240f)
            assertTrue("Vertical swipe does not toggle", graph.sameAs(render()))
            swipe(800f, 170f, 802f, 171f)
            assertTrue("Tap does not toggle", graph.sameAs(render()))
            swipe(920f, 170f, 710f, 170f, cancel = true)
            assertTrue("Cancelled swipe does not toggle", graph.sameAs(render()))
            graph.recycle()
            detail.recycle()
        }
    }
}
