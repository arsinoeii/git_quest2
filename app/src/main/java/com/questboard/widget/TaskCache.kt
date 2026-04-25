package com.questboard.widget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object TaskCache {
    private const val FILENAME = "tasks.json"

    fun save(context: Context, tasks: List<TaskData>) {
        val arr = JSONArray()
        for (t in tasks) {
            arr.put(JSONObject().apply {
                put("title", t.title)
                put("deadline", t.deadline ?: JSONObject.NULL)
                put("notionUrl", t.notionUrl)
            })
        }
        File(context.filesDir, FILENAME).writeText(arr.toString())
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
                    deadline = obj.optString("deadline").takeIf { it != "null" && it.isNotEmpty() },
                    notionUrl = obj.getString("notionUrl")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
