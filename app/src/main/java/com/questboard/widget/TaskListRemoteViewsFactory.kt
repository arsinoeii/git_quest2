package com.questboard.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TaskListRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var tasks: List<TaskData> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        tasks = TaskCache.load(context)
    }

    override fun onDestroy() {}

    override fun getCount() = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        val task = tasks[position]
        val rv = RemoteViews(context.packageName, R.layout.widget_item)
        rv.setTextViewText(R.id.task_title, task.title)

        rv.setViewVisibility(R.id.task_deadline, View.GONE)

        val fillIntent = Intent().apply {
            data = Uri.parse(task.notionUrl)
        }
        rv.setOnClickFillInIntent(R.id.item_root, fillIntent)

        return rv
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount() = 1
    override fun getItemId(position: Int) = position.toLong()
    override fun hasStableIds() = false

    private fun formatDeadline(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr.substring(0, 10))
            val today = LocalDate.now()
            val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
            when {
                days < 0 -> "Overdue by ${-days}d"
                days == 0L -> "Due today"
                days == 1L -> "Due tomorrow"
                days <= 7 -> "Due in ${days}d"
                else -> "Due ${date.format(DateTimeFormatter.ofPattern("d MMM"))}"
            }
        } catch (_: Exception) {
            dateStr
        }
    }
}
