package com.quitsq.oprclock

import android.graphics.Color

enum class DisplayTheme(
    val label: String,
    val background: Int,
    val foreground: Int,
    val secondary: Int,
    val inactiveSegment: Int,
    val grid: Int,
    val temperature: Int,
    val rain: Int
) {
    AUTO("自動（日照・天気）", Color.BLACK, Color.WHITE, 0xFFCCCCCC.toInt(), 0xFF222222.toInt(),
        0xFF414141.toInt(), 0xFFFFC15C.toInt(), 0xFF5BB4FF.toInt()),
    STANDARD("標準", Color.BLACK, Color.WHITE, 0xFFCCCCCC.toInt(), 0xFF222222.toInt(),
        0xFF414141.toInt(), 0xFFFFC15C.toInt(), 0xFF5BB4FF.toInt()),
    LIGHT("ライト", 0xFFF5F7FA.toInt(), 0xFF182230.toInt(), 0xFF4B596B.toInt(), 0xFFE0E5EC.toInt(),
        0xFFBCC6D2.toInt(), 0xFF995000.toInt(), 0xFF0068B5.toInt()),
    AMBER("アンバー", 0xFF171108.toInt(), 0xFFFFCC75.toInt(), 0xFFD4AD73.toInt(), 0xFF352919.toInt(),
        0xFF655035.toInt(), 0xFFFFB348.toInt(), 0xFF81C8EF.toInt()),
    GREEN("グリーン", 0xFF07150F.toInt(), 0xFF9CE8B4.toInt(), 0xFF8CBA9A.toInt(), 0xFF1A3526.toInt(),
        0xFF3C6249.toInt(), 0xFFFFCE76.toInt(), 0xFF77CBEF.toInt()),
    BLUE("ブルー", 0xFF091321.toInt(), 0xFFB3DAFF.toInt(), 0xFF95AEC9.toInt(), 0xFF1D3047.toInt(),
        0xFF405B79.toInt(), 0xFFFFCB7A.toInt(), 0xFF6FC8FF.toInt());

    companion object {
        fun fromName(name: String?): DisplayTheme = if (name == null) AUTO else entries.firstOrNull { it.name == name } ?: STANDARD
    }
}
