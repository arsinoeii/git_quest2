package com.questboard.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.view.View
import android.widget.RemoteViews

class QuestWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.questboard.widget.ACTION_REFRESH"
        const val ACTION_PAGE_NEXT = "com.questboard.widget.ACTION_PAGE_NEXT"
        private const val TASKS_PER_PAGE = 3
        private val TASK_IDS = intArrayOf(R.id.task1, R.id.task2, R.id.task3)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> doRefresh(context)
            ACTION_PAGE_NEXT -> {
                val tasks = TaskCache.load(context)
                val totalPages = maxOf(1, (tasks.size + TASKS_PER_PAGE - 1) / TASKS_PER_PAGE)
                val currentPage = TaskCache.getPage(context)
                TaskCache.setPage(context, (currentPage + 1) % totalPages)
                renderWidget(context, tasks)
            }
        }
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
            } catch (e: Exception) {
                val mgr = AppWidgetManager.getInstance(context)
                val ids = mgr.getAppWidgetIds(ComponentName(context, QuestWidgetProvider::class.java))
                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                views.setTextViewText(R.id.status_text, "Error: ${e.message?.take(40)}")
                views.setViewVisibility(R.id.status_text, View.VISIBLE)
                applyChrome(context, views)
                for (id in ids) mgr.updateAppWidget(id, views)
            }
        }.start()
    }

    private fun renderWidget(context: Context, tasks: List<TaskData>) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, QuestWidgetProvider::class.java))
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        views.setInt(R.id.widget_root, "setBackgroundResource", ColourUtil.todayGradientDrawable())

        views.setProgressBar(R.id.progress_bar, 100, ColourUtil.dayProgress(), false)
        try {
            views.setColorStateList(
                R.id.progress_bar, "setProgressTintList",
                ColorStateList.valueOf(ColourUtil.todayAccentColour())
            )
        } catch (_: Exception) {}

        val page = TaskCache.getPage(context)
        val startIdx = page * TASKS_PER_PAGE
        val pageTasks = tasks.drop(startIdx).take(TASKS_PER_PAGE)
        val totalPages = maxOf(1, (tasks.size + TASKS_PER_PAGE - 1) / TASKS_PER_PAGE)

        for (i in TASK_IDS.indices) {
            if (i < pageTasks.size) {
                views.setTextViewText(TASK_IDS[i], pageTasks[i].title)
                views.setViewVisibility(TASK_IDS[i], View.VISIBLE)

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

        if (totalPages > 1) {
            val dots = buildString {
                for (p in 0 until totalPages) {
                    if (p > 0) append(" ")
                    append(if (p == page) "●" else "○")
                }
            }
            views.setTextViewText(R.id.page_dots, dots)
            views.setViewVisibility(R.id.page_dots, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.page_dots, View.GONE)
        }

        applyChrome(context, views)

        for (id in ids) mgr.updateAppWidget(id, views)
    }

    private fun applyChrome(context: Context, views: RemoteViews) {
        val refreshIntent = Intent(context, QuestWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        views.setOnClickPendingIntent(R.id.refresh_button, PendingIntent.getBroadcast(
            context, 1, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ))
        views.setOnClickPendingIntent(R.id.header_area, PendingIntent.getBroadcast(
            context, 1, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ))

        val addIntent = Intent(context, AddTaskActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        views.setOnClickPendingIntent(R.id.add_button, PendingIntent.getActivity(
            context, 2, addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ))

        val pageIntent = Intent(context, QuestWidgetProvider::class.java).apply {
            action = ACTION_PAGE_NEXT
        }
        views.setOnClickPendingIntent(R.id.page_dots, PendingIntent.getBroadcast(
            context, 3, pageIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ))

        views.setOnClickPendingIntent(R.id.status_text, PendingIntent.getBroadcast(
            context, 1, Intent(context, QuestWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ))
    }
}
