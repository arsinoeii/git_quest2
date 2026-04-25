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

    fun markTaskDone(pageId: String) {
        val url = URL("https://api.notion.com/v1/pages/$pageId")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "PATCH"
        conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.NOTION_TOKEN}")
        conn.setRequestProperty("Notion-Version", "2022-06-28")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        conn.doOutput = true

        val body = JSONObject().apply {
            put("properties", JSONObject().apply {
                put("Status", JSONObject().apply {
                    put("status", JSONObject().put("name", "Done"))
                })
            })
        }

        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        if (conn.responseCode !in 200..299) {
            val err = try { BufferedReader(InputStreamReader(conn.errorStream)).use { it.readText() } } catch (_: Exception) { "" }
            throw Exception("HTTP ${conn.responseCode}: $err")
        }
    }

    data class GoalItem(val id: String, val title: String)

    fun fetchGoals(): List<GoalItem> {
        val url = URL("https://api.notion.com/v1/databases/${BuildConfig.NOTION_GOALS_DATABASE_ID}/query")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.NOTION_TOKEN}")
        conn.setRequestProperty("Notion-Version", "2022-06-28")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        conn.doOutput = true
        conn.outputStream.use { it.write("{}".toByteArray()) }

        if (conn.responseCode != 200) return emptyList()

        val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        val results = JSONObject(response).getJSONArray("results")
        val goals = mutableListOf<GoalItem>()

        for (i in 0 until results.length()) {
            val page = results.getJSONObject(i)
            val props = page.getJSONObject("properties")
            val titleProp = props.optJSONObject("Name") ?: props.optJSONObject("Goal") ?: props.optJSONObject("Title")
            val titleArr = titleProp?.optJSONArray("title") ?: continue
            val title = buildString {
                for (j in 0 until titleArr.length()) append(titleArr.getJSONObject(j).getString("plain_text"))
            }
            if (title.isBlank()) continue
            goals.add(GoalItem(page.getString("id"), title))
        }

        return goals.sortedBy { it.title }
    }

    fun createTask(
        title: String,
        phase: String,
        status: String,
        lifeArea: String?,
        deadline: String?,
        goalId: String?
    ) {
        val url = URL("https://api.notion.com/v1/pages")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.NOTION_TOKEN}")
        conn.setRequestProperty("Notion-Version", "2022-06-28")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        conn.doOutput = true

        val props = JSONObject().apply {
            put("Task", JSONObject().put("title", org.json.JSONArray().put(
                JSONObject().put("text", JSONObject().put("content", title))
            )))
            put("Phase", JSONObject().put("select", JSONObject().put("name", phase)))
            put("Status", JSONObject().put("status", JSONObject().put("name", status)))
            if (lifeArea != null) {
                put("Life Area", JSONObject().put("select", JSONObject().put("name", lifeArea)))
            }
            if (deadline != null) {
                put("Deadline", JSONObject().put("date", JSONObject().put("start", deadline)))
            }
            if (goalId != null) {
                put("Goal", JSONObject().put("relation", org.json.JSONArray().put(
                    JSONObject().put("id", goalId)
                )))
            }
        }

        val body = JSONObject().apply {
            put("parent", JSONObject().put("database_id", BuildConfig.NOTION_DATABASE_ID))
            put("properties", props)
        }

        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        if (conn.responseCode !in 200..299) {
            val err = try { BufferedReader(InputStreamReader(conn.errorStream)).use { it.readText() } } catch (_: Exception) { "" }
            throw Exception("HTTP ${conn.responseCode}: $err")
        }
    }
}
