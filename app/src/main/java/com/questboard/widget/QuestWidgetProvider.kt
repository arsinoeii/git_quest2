package com.questboard.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews

class QuestWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.questboard.widget.ACTION_REFRESH"
        const val ACTION_PAGE_NEXT = "com.questboard.widget.ACTION_PAGE_NEXT"
        const val ACTION_PAGE_PREV = "com.questboard.widget.ACTION_PAGE_PREV"
        private const val TASKS_PER_PAGE = 3
        private val TASK_IDS = intArrayOf(R.id.task1, R.id.task2, R.id.task3)

        private val DECO_BEHIND_IDS = intArrayOf(
            R.id.deco_b0, R.id.deco_b1, R.id.deco_b2, R.id.deco_b3,
            R.id.deco_b4, R.id.deco_b5, R.id.deco_b6, R.id.deco_b7,
        )
        private val DECO_TOP_IDS = intArrayOf(
            R.id.deco_t0, R.id.deco_t1, R.id.deco_t2, R.id.deco_t3,
        )

        private val MOTIFS = arrayOf(
            arrayOf("🦋", "🦋", "🦋", "🦋", "🦋", "🦋", "🦋", "🦋", "🦋", "🦋", "🦋", "🦋"),
            arrayOf("✦", "✧", "✦", "✧", "✦", "✧", "✦", "✧", "✦", "✧", "✦", "✧"),
            arrayOf("🌸", "🌸", "🌸", "🌸", "🌸", "🌸", "🌸", "🌸", "🌸", "🌸", "🌸", "🌸"),
            arrayOf("☁", "☁", "☁", "☁", "☁", "☁", "☁", "☁", "☁", "☁", "☁", "☁"),
            arrayOf("✿", "✿", "✿", "✿", "✿", "✿", "✿", "✿", "✿", "✿", "✿", "✿"),
            emptyArray(), // Lavender (PNG)
            emptyArray(), // Birds (PNG)
            arrayOf("🌿", "🍃", "🌿", "🍃", "🌿", "🍃", "🌿", "🍃", "🌿", "🍃", "🌿", "🍃"),
            arrayOf("☁", "✦", "✧", "☁", "✦", "☁", "✧", "☁", "✦", "☁", "✧", "✦"),
            arrayOf("🫧", "🫧", "🫧", "🫧", "🫧", "🫧", "🫧", "🫧", "🫧", "🫧", "🫧", "🫧"),
        )

        private val BEHIND_SIZES = floatArrayOf(18f, 12f, 16f, 14f, 10f, 17f, 13f, 15f)
        private val BEHIND_TOP_DP = intArrayOf(0, 8, 30, 55, 60, 80, 110, 135)
        private val BEHIND_START_DP = intArrayOf(2, 50, 120, 0, 95, 120, 0, 110)
        private val TOP_SIZES = floatArrayOf(12f, 13f, 9f, 10f)
        private val TOP_TOP_DP = intArrayOf(42, 72, 100, 128)
        private val TOP_START_DP = intArrayOf(45, 100, 48, 85)
        private val BEHIND_ALPHA = intArrayOf(102, 89, 97, 92, 77, 102, 87, 92)
        private val TOP_ALPHA = intArrayOf(82, 87, 72, 77)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> doRefresh(context)
            ACTION_PAGE_NEXT -> changePage(context, 1)
            ACTION_PAGE_PREV -> changePage(context, -1)
        }
    }

    private fun changePage(context: Context, direction: Int) {
        val tasks = TaskCache.load(context)
        val totalPages = maxOf(1, (tasks.size + TASKS_PER_PAGE - 1) / TASKS_PER_PAGE)
        val currentPage = TaskCache.getPage(context)
        TaskCache.setPage(context, (currentPage + direction + totalPages) % totalPages)
        renderWidget(context, tasks)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        doRefresh(context)
    }

    private fun doRefresh(context: Context) {
        Thread {
            try {
                val tasks = NotionApi.fetchTodayTasks()
                TaskCache.save(context, tasks)
                renderWidget(context, tasks)
            } catch (_: Exception) {
                val cached = TaskCache.load(context)
                if (cached.isNotEmpty()) {
                    renderWidget(context, cached)
                } else {
                    val mgr = AppWidgetManager.getInstance(context)
                    val ids = mgr.getAppWidgetIds(ComponentName(context, QuestWidgetProvider::class.java))
                    val views = RemoteViews(context.packageName, R.layout.widget_layout)
                    views.setTextViewText(R.id.status_text, "Tap ⟳ to sync")
                    views.setViewVisibility(R.id.status_text, View.VISIBLE)
                    applyTheme(context, views)
                    applyChrome(context, views)
                    for (id in ids) mgr.updateAppWidget(id, views)
                }
            }
        }.start()
    }

    private fun renderWidget(context: Context, tasks: List<TaskData>) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, QuestWidgetProvider::class.java))
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        applyTheme(context, views)

        val page = TaskCache.getPage(context)
        val startIdx = page * TASKS_PER_PAGE
        val pageTasks = tasks.drop(startIdx).take(TASKS_PER_PAGE)
        val totalPages = maxOf(1, (tasks.size + TASKS_PER_PAGE - 1) / TASKS_PER_PAGE)
        val theme = ColourUtil.todayTheme()

        for (i in TASK_IDS.indices) {
            if (i < pageTasks.size) {
                views.setTextViewText(TASK_IDS[i], pageTasks[i].title)
                views.setViewVisibility(TASK_IDS[i], View.VISIBLE)
                views.setTextColor(TASK_IDS[i], theme.taskTextColour)

                val taskIntent = Intent(context, TaskActionActivity::class.java).apply {
                    putExtra("title", pageTasks[i].title)
                    putExtra("pageId", pageTasks[i].pageId)
                    putExtra("notionUrl", pageTasks[i].notionUrl)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val taskPending = PendingIntent.getActivity(
                    context, startIdx + i + 100, taskIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(TASK_IDS[i], taskPending)
            } else {
                views.setViewVisibility(TASK_IDS[i], View.GONE)
            }
        }

        if (tasks.isEmpty()) {
            views.setTextViewText(R.id.status_text, "Nothing for today")
            views.setViewVisibility(R.id.status_text, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.status_text, View.GONE)
        }

        // Dots — always visible
        val dots = buildString {
            for (p in 0 until totalPages) {
                if (p > 0) append("  ")
                append(if (p == page) "●" else "○")
            }
        }
        views.setTextViewText(R.id.page_dots, dots)
        views.setTextColor(R.id.page_dots, withAlpha(theme.textColour, 153))

        // Show arrows only when there are multiple pages
        if (totalPages > 1) {
            views.setViewVisibility(R.id.nav_prev, View.VISIBLE)
            views.setViewVisibility(R.id.nav_next, View.VISIBLE)
            views.setTextColor(R.id.nav_prev, withAlpha(theme.textColour, 153))
            views.setTextColor(R.id.nav_next, withAlpha(theme.textColour, 153))
        } else {
            views.setViewVisibility(R.id.nav_prev, View.INVISIBLE)
            views.setViewVisibility(R.id.nav_next, View.INVISIBLE)
        }

        applyChrome(context, views)

        for (id in ids) mgr.updateAppWidget(id, views)
    }

    private fun applyTheme(context: Context, views: RemoteViews) {
        val theme = ColourUtil.todayTheme()
        val density = context.resources.displayMetrics.density

        views.setInt(R.id.widget_root, "setBackgroundResource", theme.gradientDrawable)
        views.setTextColor(R.id.widget_title, theme.textColour)
        views.setTextColor(R.id.status_text, withAlpha(theme.textColour, 102))

        // Both + and sync get the same circle style
        views.setTextColor(R.id.add_button, Color.WHITE)
        views.setTextColor(R.id.refresh_button, Color.WHITE)
        try {
            val tint = ColorStateList.valueOf(theme.buttonColour)
            views.setColorStateList(R.id.add_button, "setBackgroundTintList", tint)
            views.setColorStateList(R.id.refresh_button, "setBackgroundTintList", tint)
        } catch (_: Exception) {}

        views.setProgressBar(R.id.progress_bar, 100, ColourUtil.dayProgress(), false)
        try {
            views.setColorStateList(
                R.id.progress_bar, "setProgressTintList",
                ColorStateList.valueOf(theme.progressColour)
            )
        } catch (_: Exception) {}

        applyDecorations(context, views, theme, density)
    }

    private fun applyDecorations(@Suppress("UNUSED_PARAMETER") context: Context, views: RemoteViews, theme: ColourUtil.DayTheme, density: Float) {
        val mi = theme.motifIndex
        val motifChars = MOTIFS[mi]
        val isPng = motifChars.isEmpty()
        val isCloud = mi == 3
        val isSparkle = mi == 1
        val isJasmine = mi == 4
        val isCombo = mi == 8
        val usesDecoColour = isSparkle || isJasmine

        for (i in DECO_BEHIND_IDS.indices) {
            val id = DECO_BEHIND_IDS[i]
            if (isPng) { views.setViewVisibility(id, View.GONE); continue }
            val char = motifChars[i % motifChars.size]
            views.setTextViewText(id, char)
            views.setViewVisibility(id, View.VISIBLE)
            views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, BEHIND_SIZES[i])
            views.setViewPadding(id, (BEHIND_START_DP[i] * density).toInt(), (BEHIND_TOP_DP[i] * density).toInt(), 0, 0)
            val colour = when {
                isCloud -> withAlpha(Color.WHITE, BEHIND_ALPHA[i] + 60)
                isCombo && char == "☁" -> withAlpha(Color.WHITE, BEHIND_ALPHA[i] + 60)
                isCombo -> withAlpha(theme.decoColour, 255)
                usesDecoColour -> withAlpha(theme.decoColour, 255)
                else -> withAlpha(theme.decoColour, BEHIND_ALPHA[i])
            }
            views.setTextColor(id, colour)
        }

        for (i in DECO_TOP_IDS.indices) {
            val id = DECO_TOP_IDS[i]
            if (isPng) { views.setViewVisibility(id, View.GONE); continue }
            val char = motifChars[(DECO_BEHIND_IDS.size + i) % motifChars.size]
            views.setTextViewText(id, char)
            views.setViewVisibility(id, View.VISIBLE)
            views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, TOP_SIZES[i])
            views.setViewPadding(id, (TOP_START_DP[i] * density).toInt(), (TOP_TOP_DP[i] * density).toInt(), 0, 0)
            val colour = when {
                isCloud -> withAlpha(Color.WHITE, TOP_ALPHA[i] + 60)
                isCombo && char == "☁" -> withAlpha(Color.WHITE, TOP_ALPHA[i] + 60)
                isCombo -> withAlpha(theme.decoColour, 255)
                usesDecoColour -> withAlpha(theme.decoColour, 255)
                else -> withAlpha(theme.decoColour, TOP_ALPHA[i])
            }
            views.setTextColor(id, colour)
        }

        val imgBehind = intArrayOf(R.id.deco_img_b0, R.id.deco_img_b1, R.id.deco_img_b2, R.id.deco_img_b3)
        val imgTop = intArrayOf(R.id.deco_img_t0, R.id.deco_img_t1)

        if (mi == 5) {
            val sprigSizes = intArrayOf(36, 28, 32, 30)
            val sprigTops = intArrayOf(0, 30, 65, 110)
            val sprigStarts = intArrayOf(2, 100, 0, 95)
            for (i in imgBehind.indices) {
                val bmp = drawLavenderSprig((sprigSizes[i] * density).toInt(), withAlpha(0xFF8c64b4.toInt(), 102))
                views.setImageViewBitmap(imgBehind[i], bmp)
                views.setViewVisibility(imgBehind[i], View.VISIBLE)
                views.setViewPadding(imgBehind[i], (sprigStarts[i] * density).toInt(), (sprigTops[i] * density).toInt(), 0, 0)
            }
            for (i in imgTop.indices) {
                val bmp = drawLavenderSprig((26 * density).toInt(), withAlpha(0xFF8c64b4.toInt(), 87))
                views.setImageViewBitmap(imgTop[i], bmp)
                views.setViewVisibility(imgTop[i], View.VISIBLE)
                views.setViewPadding(imgTop[i], (intArrayOf(42, 85)[i] * density).toInt(), (intArrayOf(45, 100)[i] * density).toInt(), 0, 0)
            }
        } else if (mi == 6) {
            val birdSizes = intArrayOf(24, 16, 22, 18)
            val birdTops = intArrayOf(2, 30, 65, 110)
            val birdStarts = intArrayOf(4, 100, 0, 95)
            for (i in imgBehind.indices) {
                val w = (birdSizes[i] * density).toInt()
                val bmp = drawBirdSilhouette(w, (w * 0.58f).toInt(), withAlpha(theme.decoColour, 130))
                views.setImageViewBitmap(imgBehind[i], bmp)
                views.setViewVisibility(imgBehind[i], View.VISIBLE)
                views.setViewPadding(imgBehind[i], (birdStarts[i] * density).toInt(), (birdTops[i] * density).toInt(), 0, 0)
            }
            for (i in imgTop.indices) {
                val w = (16 * density).toInt()
                val bmp = drawBirdSilhouette(w, (w * 0.58f).toInt(), withAlpha(theme.decoColour, 102))
                views.setImageViewBitmap(imgTop[i], bmp)
                views.setViewVisibility(imgTop[i], View.VISIBLE)
                views.setViewPadding(imgTop[i], (intArrayOf(40, 85)[i] * density).toInt(), (intArrayOf(42, 100)[i] * density).toInt(), 0, 0)
            }
        } else {
            for (id in imgBehind + imgTop) views.setViewVisibility(id, View.GONE)
        }
    }

    private fun applyChrome(context: Context, views: RemoteViews) {
        val refreshIntent = Intent(context, QuestWidgetProvider::class.java).apply { action = ACTION_REFRESH }
        val refreshPending = PendingIntent.getBroadcast(context, 1, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.refresh_button, refreshPending)

        val addIntent = Intent(context, AddTaskActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        views.setOnClickPendingIntent(R.id.add_button, PendingIntent.getActivity(context, 2, addIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        val nextIntent = Intent(context, QuestWidgetProvider::class.java).apply { action = ACTION_PAGE_NEXT }
        views.setOnClickPendingIntent(R.id.nav_next, PendingIntent.getBroadcast(context, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        views.setOnClickPendingIntent(R.id.page_dots, PendingIntent.getBroadcast(context, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        val prevIntent = Intent(context, QuestWidgetProvider::class.java).apply { action = ACTION_PAGE_PREV }
        views.setOnClickPendingIntent(R.id.nav_prev, PendingIntent.getBroadcast(context, 4, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        views.setOnClickPendingIntent(R.id.status_text, refreshPending)
    }

    private fun drawLavenderSprig(height: Int, colour: Int): Bitmap {
        val w = (height * 0.45f).toInt().coerceAtLeast(4)
        val bmp = Bitmap.createBitmap(w, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colour }
        val cx = w / 2f
        paint.strokeWidth = 1.5f; paint.style = Paint.Style.STROKE
        canvas.drawLine(cx, height.toFloat(), cx, height * 0.2f, paint)
        paint.style = Paint.Style.FILL
        val budW = w * 0.35f; val budH = w * 0.5f
        for ((i, pos) in floatArrayOf(0.2f, 0.32f, 0.44f, 0.56f).withIndex()) {
            val y = height * pos; val ox = if (i % 2 == 0) -budW * 0.6f else budW * 0.6f
            canvas.drawOval(cx + ox - budW / 2, y - budH / 2, cx + ox + budW / 2, y + budH / 2, paint)
        }
        canvas.drawOval(cx - budW * 0.4f, height * 0.1f, cx + budW * 0.4f, height * 0.22f, paint)
        return bmp
    }

    private fun drawBirdSilhouette(width: Int, height: Int, colour: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colour; style = Paint.Style.STROKE; strokeWidth = 2f; strokeCap = Paint.Cap.ROUND
        }
        val cx = width / 2f
        val path = android.graphics.Path()
        path.moveTo(0f, height * 0.35f); path.quadTo(cx * 0.5f, height * 0.15f, cx, height * 0.7f)
        path.moveTo(width.toFloat(), height * 0.35f); path.quadTo(cx * 1.5f, height * 0.15f, cx, height * 0.7f)
        canvas.drawPath(path, paint)
        return bmp
    }

    private fun withAlpha(colour: Int, alpha: Int): Int =
        (colour and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
}
