package com.questboard.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews

class QuestWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.questboard.widget.ACTION_REFRESH"

        private val TASK_IDS = intArrayOf(
            R.id.task1, R.id.task2, R.id.task3, R.id.task4,
            R.id.task5, R.id.task6, R.id.task7, R.id.task8
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            doRefresh(context)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            setupWidget(context, manager, id)
        }
        doRefresh(context)
    }

    private fun setupWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        val refreshIntent = Intent(context, QuestWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPending = PendingIntent.getBroadcast(
            context, 1, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.refresh_button, refreshPending)
        views.setOnClickPendingIntent(R.id.header_area, refreshPending)
        views.setOnClickPendingIntent(R.id.status_text, refreshPending)

        views.setTextViewText(R.id.status_text, "Loading...")

        manager.updateAppWidget(widgetId, views)
    }

    private fun doRefresh(context: Context) {
        Thread {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(
                ComponentName(context, QuestWidgetProvider::class.java)
            )

            try {
                val tasks = NotionApi.fetchTodayTasks()
                TaskCache.save(context, tasks)

                val views = RemoteViews(context.packageName, R.layout.widget_layout)

                for (i in TASK_IDS.indices) {
                    if (i < tasks.size) {
                        views.setTextViewText(TASK_IDS[i], tasks[i].title)
                        views.setViewVisibility(TASK_IDS[i], View.VISIBLE)

                        val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tasks[i].notionUrl))
                        val openPending = PendingIntent.getActivity(
                            context, i + 10, openIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(TASK_IDS[i], openPending)
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

                val refreshIntent = Intent(context, QuestWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                }
                val refreshPending = PendingIntent.getBroadcast(
                    context, 1, refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.refresh_button, refreshPending)
                views.setOnClickPendingIntent(R.id.header_area, refreshPending)
                views.setOnClickPendingIntent(R.id.status_text, refreshPending)

                for (id in ids) {
                    mgr.updateAppWidget(id, views)
                }
            } catch (e: Exception) {
                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                views.setTextViewText(R.id.status_text, "Error: ${e.message?.take(50)}")
                views.setViewVisibility(R.id.status_text, View.VISIBLE)

                val refreshIntent = Intent(context, QuestWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                }
                val refreshPending = PendingIntent.getBroadcast(
                    context, 1, refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.refresh_button, refreshPending)
                views.setOnClickPendingIntent(R.id.header_area, refreshPending)
                views.setOnClickPendingIntent(R.id.status_text, refreshPending)

                for (id in ids) {
                    mgr.updateAppWidget(id, views)
                }
            }
        }.start()
    }

    private fun formatDeadline(dateStr: String): String {
        return try {
            val date = java.time.LocalDate.parse(dateStr.substring(0, 10))
            val today = java.time.LocalDate.now()
            val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
            when {
                days < 0 -> "⚠ Overdue by ${-days}d"
                days == 0L -> "Due today"
                days == 1L -> "Due tomorrow"
                days <= 7 -> "Due in ${days}d"
                else -> "Due ${date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))}"
            }
        } catch (_: Exception) {
            dateStr
        }
    }
}
