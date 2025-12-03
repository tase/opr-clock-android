package com.quitsq.oprclock

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var clockView: SignageClockView

    private val updateHandler = Handler(Looper.getMainLooper())
    private val syncHandler = Handler(Looper.getMainLooper())

    private val trueTime = TrueTimeProvider()

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateTime()
            val now = trueTime.nowMillis()
            val delay = 1000 - (now % 1000)
            updateHandler.postDelayed(this, delay)
        }
    }

    private val syncRunnable = object : Runnable {
        override fun run() {
            trueTime.syncOnceAsync()
            syncHandler.postDelayed(this, 10 * 60 * 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        clockView = findViewById(R.id.signage_clock)
        hideSystemUi()

        trueTime.syncOnceAsync()
    }

    override fun onResume() {
        super.onResume()
        updateHandler.post(updateRunnable)
        syncHandler.post(syncRunnable)
    }

    override fun onPause() {
        super.onPause()
        updateHandler.removeCallbacks(updateRunnable)
        syncHandler.removeCallbacks(syncRunnable)
    }

    private fun updateTime() {
        val jst = trueTime.nowJst()
        val utc = trueTime.nowUtc()
        clockView.setTime(jst, utc)
    }

    private fun hideSystemUi() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN
    }
}
