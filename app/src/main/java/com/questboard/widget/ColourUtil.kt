package com.questboard.widget

import java.util.Calendar

object ColourUtil {

    private val ACCENT_COLOURS = intArrayOf(
        0xFFe94560.toInt(), // 0  Coral
        0xFF06b6d4.toInt(), // 1  Teal
        0xFFf59e0b.toInt(), // 2  Amber
        0xFF8b5cf6.toInt(), // 3  Purple
        0xFF3b82f6.toInt(), // 4  Blue
        0xFFec4899.toInt(), // 5  Pink
        0xFF10b981.toInt(), // 6  Green
        0xFFef4444.toInt(), // 7  Red
        0xFFa78bfa.toInt(), // 8  Lavender
        0xFF34d399.toInt(), // 9  Emerald
        0xFFf472b6.toInt(), // 10 Rose
        0xFF22d3ee.toInt(), // 11 Cyan
        0xFFf97316.toInt(), // 12 Orange
        0xFF6366f1.toInt(), // 13 Indigo
        0xFF84cc16.toInt(), // 14 Lime
        0xFFd946ef.toInt(), // 15 Fuchsia
        0xFF38bdf8.toInt(), // 16 Sky
        0xFFeab308.toInt(), // 17 Yellow
        0xFF6482ff.toInt(), // 18 Slate Blue
        0xFF2dd4bf.toInt(), // 19 Mint
    )

    private val GRADIENT_DRAWABLES = intArrayOf(
        R.drawable.gradient_0,  R.drawable.gradient_1,  R.drawable.gradient_2,
        R.drawable.gradient_3,  R.drawable.gradient_4,  R.drawable.gradient_5,
        R.drawable.gradient_6,  R.drawable.gradient_7,  R.drawable.gradient_8,
        R.drawable.gradient_9,  R.drawable.gradient_10, R.drawable.gradient_11,
        R.drawable.gradient_12, R.drawable.gradient_13, R.drawable.gradient_14,
        R.drawable.gradient_15, R.drawable.gradient_16, R.drawable.gradient_17,
        R.drawable.gradient_18, R.drawable.gradient_19,
    )

    fun todayIndex(): Int {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val hash = (dayOfYear.toLong() * 2654435761L).toInt()
        return (hash and 0x7FFFFFFF) % ACCENT_COLOURS.size
    }

    fun todayAccentColour(): Int = ACCENT_COLOURS[todayIndex()]

    fun todayGradientDrawable(): Int = GRADIENT_DRAWABLES[todayIndex()]

    fun dayProgress(): Int {
        val cal = Calendar.getInstance()
        val minutesSince9am = (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)) - 540
        val progress = (minutesSince9am * 100) / 900
        return progress.coerceIn(0, 100)
    }
}
