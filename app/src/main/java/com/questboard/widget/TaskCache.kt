package com.questboard.widget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object TaskCache {
    private const val FILENAME = "tasks.json"
    private const val PREFS = "quest_widget_prefs"
    private const val KEY_PAGE = "current_page"

    fun save(context: Context, tasks: List<TaskData>) {
        val arr = JSONArray()
        for (t in tasks) {
            arr.put(JSONObject().apply {
                put("title", t.title)
                put("pageId", t.pageId)
                put("notionUrl", t.notionUrl)
            })
        }
        File(context.filesDir, FILENAME).writeText(arr.toString())
        setPage(context, 0)
    }

    fun load(context: Context): List<TaskData> {
        val file = File(context.filesDir, FILENAME)
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                TaskData(
                    title = obj.getString("title"),
                    pageId = obj.getString("pageId"),
                    notionUrl = obj.getString("notionUrl")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getPage(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_PAGE, 0)
    }

    fun setPage(context: Context, page: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_PAGE, page).apply()
    }
}
