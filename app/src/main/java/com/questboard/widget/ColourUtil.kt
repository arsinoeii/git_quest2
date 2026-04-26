package com.questboard.widget

import java.util.Calendar

object ColourUtil {

    private const val NUM_COLOURS = 10

    // Sage, Blush, Cream, Sand, Peach, Lavender, Powder, Mint, Mauve, Dove
    private val TEXT_COLOURS = intArrayOf(
        0xFF4a5940.toInt(), 0xFF7a4a40.toInt(), 0xFF6a5a3a.toInt(),
        0xFF6a5840.toInt(), 0xFF7a5040.toInt(), 0xFF5a4070.toInt(),
        0xFF3a5060.toInt(), 0xFF3a5a50.toInt(), 0xFF6a4058.toInt(),
        0xFF505050.toInt(),
    )

    private val TASK_TEXT_COLOURS = intArrayOf(
        0xFF3a4830.toInt(), 0xFF6a3e34.toInt(), 0xFF5a4a2e.toInt(),
        0xFF5a4832.toInt(), 0xFF6a4434.toInt(), 0xFF4a3060.toInt(),
        0xFF2e4452.toInt(), 0xFF2e4a42.toInt(), 0xFF5a3048.toInt(),
        0xFF404040.toInt(),
    )

    private val BUTTON_COLOURS = intArrayOf(
        0xFF8aab72.toInt(), 0xFFc88a7a.toInt(), 0xFFb8a478.toInt(),
        0xFFb09878.toInt(), 0xFFc89478.toInt(), 0xFFa088c0.toInt(),
        0xFF78a0b8.toInt(), 0xFF70ac98.toInt(), 0xFFb4889c.toInt(),
        0xFF989090.toInt(),
    )

    private val PROGRESS_START = intArrayOf(
        0xFFa8bf94.toInt(), 0xFFdaa090.toInt(), 0xFFc8b890.toInt(),
        0xFFc4b090.toInt(), 0xFFd8a88c.toInt(), 0xFFb8a0d0.toInt(),
        0xFF90b4cc.toInt(), 0xFF88c0ac.toInt(), 0xFFc8a0b4.toInt(),
        0xFFa8a0a0.toInt(),
    )

    private val DECO_COLOURS = intArrayOf(
        0xFF4a5940.toInt(), 0xFF7a4a40.toInt(), 0xFF6a5a3a.toInt(),
        0xFF6a5840.toInt(), 0xFF7a5040.toInt(), 0xFF5a4070.toInt(),
        0xFF3a5060.toInt(), 0xFF3a5a50.toInt(), 0xFF6a4058.toInt(),
        0xFF505050.toInt(),
    )

    private val GRADIENT_DRAWABLES = intArrayOf(
        R.drawable.gradient_0, R.drawable.gradient_1, R.drawable.gradient_2,
        R.drawable.gradient_3, R.drawable.gradient_4, R.drawable.gradient_5,
        R.drawable.gradient_6, R.drawable.gradient_7, R.drawable.gradient_8,
        R.drawable.gradient_9,
    )

    data class DayTheme(
        val gradientDrawable: Int,
        val textColour: Int,
        val taskTextColour: Int,
        val buttonColour: Int,
        val progressColour: Int,
        val decoColour: Int,
        val motifIndex: Int,
    )

    fun todayIndex(): Int {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val hash = (dayOfYear.toLong() * 2654435761L).toInt()
        return (hash and 0x7FFFFFFF) % NUM_COLOURS
    }

    fun todayMotifIndex(): Int {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val hash = (dayOfYear.toLong() * 2654435761L).toInt()
        return ((hash and 0x7FFFFFFF) + 7) % NUM_COLOURS
    }

    fun todayTheme(): DayTheme {
        val ci = todayIndex()
        val mi = todayMotifIndex()
        return DayTheme(
            gradientDrawable = GRADIENT_DRAWABLES[ci],
            textColour = TEXT_COLOURS[ci],
            taskTextColour = TASK_TEXT_COLOURS[ci],
            buttonColour = BUTTON_COLOURS[ci],
            progressColour = PROGRESS_START[ci],
            decoColour = DECO_COLOURS[ci],
            motifIndex = mi,
        )
    }

    fun todayAccentColour(): Int = BUTTON_COLOURS[todayIndex()]
    fun todayGradientDrawable(): Int = GRADIENT_DRAWABLES[todayIndex()]

    fun dayProgress(): Int {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        if (hour < 9) return 100
        val minutesSince9am = (hour * 60 + cal.get(Calendar.MINUTE)) - 540
        val progress = (minutesSince9am * 100) / 900
        return progress.coerceIn(0, 100)
    }
}
