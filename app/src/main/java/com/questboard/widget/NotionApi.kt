package com.questboard.widget

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object NotionApi {

    fun fetchTodayTasks(): List<TaskData> {
        val url = URL("https://api.notion.com/v1/databases/${BuildConfig.NOTION_DATABASE_ID}/query")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.NOTION_TOKEN}")
        conn.setRequestProperty("Notion-Version", "2022-06-28")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        conn.doOutput = true

        val body = JSONObject().apply {
            put("filter", JSONObject().apply {
                put("property", "Status")
                put("status", JSONObject().put("equals", BuildConfig.NOTION_STATUS_VALUE))
            })
            put("page_size", 50)
        }

        conn.outputStream.use { it.write(body.toString().toByteArray()) }

        if (conn.responseCode != 200) return emptyList()

        val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        val json = JSONObject(response)
        val results = json.getJSONArray("results")
        val tasks = mutableListOf<TaskData>()

        for (i in 0 until results.length()) {
            val page = results.getJSONObject(i)
            val props = page.getJSONObject("properties")

            val titleArray = props.getJSONObject("Task").getJSONArray("title")
            val title = buildString {
                for (j in 0 until titleArray.length()) {
                    append(titleArray.getJSONObject(j).getString("plain_text"))
                }
            }
            if (title.isBlank()) continue

            val pageId = page.getString("id")
            val notionUrl = "https://notion.so/${pageId.replace("-", "")}"

            tasks.add(TaskData(title, pageId, notionUrl))
        }

        return tasks
    }

    fun markTaskDone(pageId: String) { /* stub — implemented in Task 6 */ }
}
