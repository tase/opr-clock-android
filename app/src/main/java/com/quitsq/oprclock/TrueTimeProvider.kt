package com.quitsq.oprclock

import android.os.SystemClock
import java.util.Calendar
import java.util.TimeZone
import kotlin.concurrent.thread

class TrueTimeProvider {

    private val sntpClient = SntpClient()

    @Volatile
    private var lastNtpTimeMillis: Long = 0L

    @Volatile
    private var lastNtpReferenceElapsed: Long = 0L

    /** NTP同期時刻またはSystem.currentTimeMillis()を返す */
    fun nowMillis(): Long {
        return if (lastNtpTimeMillis != 0L) {
            val delta = SystemClock.elapsedRealtime() - lastNtpReferenceElapsed
            lastNtpTimeMillis + delta
        } else {
            System.currentTimeMillis()
        }
    }

    /** JST の Calendar を返す */
    fun nowJst(): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo")).apply {
            timeInMillis = nowMillis()
        }

    /** UTC の Calendar を返す */
    fun nowUtc(): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = nowMillis()
        }

    /** バックグラウンドで1回だけ NTP 同期 */
    fun syncOnceAsync(
        host: String = "ntp.nict.jp",
        timeoutMillis: Int = 3000,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        thread {
            try {
                val result = sntpClient.requestTime(host, timeoutMillis)
                if (result != null) {
                    lastNtpTimeMillis = result.ntpTimeMillis
                    lastNtpReferenceElapsed = result.ntpTimeReferenceMillis
                    onResult?.invoke(true)
                } else {
                    onResult?.invoke(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult?.invoke(false)
            }
        }
    }
}
