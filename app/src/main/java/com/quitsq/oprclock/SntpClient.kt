package com.quitsq.oprclock

import android.os.SystemClock
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class SntpClient {

    data class Result(
        val ntpTimeMillis: Long,
        val ntpTimeReferenceMillis: Long
    )

    fun requestTime(
        host: String = "ntp.nict.jp",
        timeoutMillis: Int = 3000
    ): Result? {
        val buffer = ByteArray(48)
        buffer[0] = 0b00_100_011 // LI=0, VN=4, Mode=3 (client)

        val address = InetAddress.getByName(host)
        val requestPacket = DatagramPacket(buffer, buffer.size, address, 123)

        DatagramSocket().use { socket ->
            socket.soTimeout = timeoutMillis
            val requestTime = SystemClock.elapsedRealtime()

            socket.send(requestPacket)

            val responsePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(responsePacket)
            val responseTime = SystemClock.elapsedRealtime()

            // NTP時刻はバイト40〜47に格納（秒 + 小数）
            val transmitTime = readTimestamp(buffer, 40)

            if (transmitTime == 0L) return null

            // NTP時刻(1900年基準) -> Unix epoch(1970年基準) に変換
            val ntpTimeMillis = transmitTime - OFFSET_1900_TO_1970

            // 往復遅延をざっくり補正して中点を基準にする
            val roundTrip = responseTime - requestTime
            val reference = responseTime - roundTrip / 2

            return Result(
                ntpTimeMillis = ntpTimeMillis,
                ntpTimeReferenceMillis = reference
            )
        }
    }

    private fun readTimestamp(buffer: ByteArray, offset: Int): Long {
        // 32bit 秒 + 32bit 小数 → ミリ秒
        val seconds = read32(buffer, offset).toLong() and 0xffffffffL
        val fraction = read32(buffer, offset + 4).toLong() and 0xffffffffL
        return (seconds * 1000L) + ((fraction * 1000L) shr 32)
    }

    private fun read32(buffer: ByteArray, offset: Int): Int {
        var value = 0
        for (i in 0 until 4) {
            value = (value shl 8) or (buffer[offset + i].toInt() and 0xff)
        }
        return value
    }

    companion object {
        // 1900-01-01 00:00:00 と 1970-01-01 00:00:00 の差（ミリ秒）
        private const val OFFSET_1900_TO_1970 = (2208988800L * 1000L)
    }
}
