package com.questboard.widget

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast

class TaskActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_action)

        val title = intent.getStringExtra("title") ?: ""
        val pageId = intent.getStringExtra("pageId") ?: ""
        val notionUrl = intent.getStringExtra("notionUrl") ?: ""

        findViewById<TextView>(R.id.task_title).text = title

        findViewById<android.view.View>(R.id.overlay_root).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.card).setOnClickListener { /* consume */ }

        findViewById<TextView>(R.id.btn_open).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(notionUrl)))
            finish()
        }

        findViewById<TextView>(R.id.btn_done).setOnClickListener {
            it.isEnabled = false
            (it as TextView).text = "..."
            Thread {
                try {
                    NotionApi.markTaskDone(pageId)
                    sendBroadcast(Intent(this, QuestWidgetProvider::class.java).apply {
                        action = QuestWidgetProvider.ACTION_REFRESH
                    })
                    runOnUiThread { finish() }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        (it as TextView).text = "Done ✓"
                        it.isEnabled = true
                    }
                }
            }.start()
        }
    }
}
