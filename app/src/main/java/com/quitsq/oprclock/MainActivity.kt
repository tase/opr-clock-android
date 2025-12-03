package com.quitsq.oprclock

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var clockView: SignageClockView

    private val updateHandler = Handler(Looper.getMainLooper())
    private val syncHandler = Handler(Looper.getMainLooper())

    private val rssExecutor = Executors.newSingleThreadExecutor()
    private val radarExecutor = Executors.newSingleThreadExecutor()
    private val forecastExecutor = Executors.newSingleThreadExecutor()
    private val forecastClient = ForecastClient()
    private val forecastRunnable = object : Runnable {
        override fun run() {
            val generation = rssGeneration
            forecastExecutor.execute {
                val result = runCatching { forecastClient.fetch() }
                updateHandler.post {
                    if (rssActive && generation == rssGeneration) {
                        result.onSuccess { clockView.setForecast(it) }.onFailure {
                            Log.w("ForecastClient", "Failed to refresh forecast", it)
                            clockView.setForecastFailed()
                        }
                        updateHandler.postDelayed(this, 30 * 60_000L)
                    }
                }
            }
        }
    }
    private val radarClient = RadarClient()
    private val radarRunnable = object : Runnable {
        override fun run() {
            val generation = rssGeneration
            radarExecutor.execute {
                val result = runCatching { radarClient.fetch() }
                updateHandler.post {
                    if (rssActive && generation == rssGeneration) {
                        result.onSuccess { clockView.setRadar(it) }.onFailure {
                            Log.w("RadarClient", "Failed to refresh radar", it)
                            clockView.setRadarFailed()
                        }
                        updateHandler.postDelayed(this, 5 * 60_000L)
                    } else {
                        result.getOrNull()?.bitmap?.recycle()
                    }
                }
            }
        }
    }

    private val rssClient = RssClient()
    private var headlines = emptyList<String>()
    private var rssActive = false
    private var rssGeneration = 0

    private val rssRunnable = object : Runnable {
        override fun run() {
            val generation = rssGeneration
            rssExecutor.execute {
                val result = runCatching { rssClient.fetch(getString(R.string.rss_feed_url)) }
                updateHandler.post {
                    if (rssActive && generation == rssGeneration) {
                        result.onSuccess {
                            headlines = it
                            clockView.setHeadlines(it)
                        }.onFailure { error ->
                            Log.w("RssClient", "Failed to refresh RSS", error)
                            if (headlines.isEmpty()) {
                                clockView.setHeadline(getString(R.string.rss_failed))
                            }
                        }
                        updateHandler.postDelayed(this, 5 * 60_000L)
                    }
                }
            }
        }
    }

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
        clockView.setHeadline(getString(R.string.rss_loading))

        trueTime.syncOnceAsync()
    }

    override fun onResume() {
        super.onResume()
        rssActive = true
        updateHandler.post(rssRunnable)
        updateHandler.post(radarRunnable)
        updateHandler.post(forecastRunnable)
        clockView.setScrolling(true)
        updateHandler.post(updateRunnable)
        syncHandler.post(syncRunnable)
    }

    override fun onPause() {
        super.onPause()
        rssActive = false
        rssGeneration++
        updateHandler.removeCallbacks(rssRunnable)
        updateHandler.removeCallbacks(radarRunnable)
        updateHandler.removeCallbacks(forecastRunnable)
        clockView.setScrolling(false)
        updateHandler.removeCallbacks(updateRunnable)
        syncHandler.removeCallbacks(syncRunnable)
    }

    override fun onDestroy() {
        rssExecutor.shutdownNow()
        radarExecutor.shutdownNow()
        forecastExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun updateTime() {
        val jst = trueTime.nowJst()
        clockView.setTime(jst)
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
