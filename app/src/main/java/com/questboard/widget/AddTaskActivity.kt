package com.questboard.widget

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class AddTaskActivity : Activity() {

    private var selectedPhase = "🏁 This Week"
    private var selectedStatus = "Today"
    private var selectedLifeArea: String? = null
    private var selectedDeadline: String? = null
    private var selectedGoalId: String? = null
    private var selectedGoalTitle: String? = null
    private var goals: List<NotionApi.GoalItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        findViewById<android.view.View>(R.id.overlay_root).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.card).setOnClickListener { /* consume */ }

        setupChips(
            R.id.chips_phase,
            listOf("🏁 This Week", "This Month", "🧠 Dump", "This Quarter", "This Year"),
            selectedPhase
        ) { selectedPhase = it }

        setupChips(
            R.id.chips_status,
            listOf("Today", "To Do", "Not Started"),
            selectedStatus
        ) { selectedStatus = it }

        val lifeAreas = listOf(
            "🧾 Life Admin", "💼 Career", "💬🫶 Relationships",
            "🧘‍♀️ Health & Wellbeing", "🎨 Joy & Creativity", "💸 Finances",
            "🪷 Home & Space", "🎱 Miscellaneous"
        )
        setupChips(R.id.chips_life_area, lifeAreas, null) { selectedLifeArea = it }

        findViewById<TextView>(R.id.input_deadline).setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                selectedDeadline = "%04d-%02d-%02d".format(y, m + 1, d)
                (it as TextView).text = selectedDeadline
                (it as TextView).setTextColor(0xFFFFFFFF.toInt())
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        findViewById<TextView>(R.id.input_goal).setOnClickListener { v ->
            if (goals.isEmpty()) {
                (v as TextView).text = "Loading..."
                Thread {
                    try {
                        goals = NotionApi.fetchGoals()
                        runOnUiThread { showGoalPicker(v as TextView) }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this, "Failed to load goals", Toast.LENGTH_SHORT).show()
                            (v as TextView).text = "Tap to select"
                        }
                    }
                }.start()
            } else {
                showGoalPicker(v as TextView)
            }
        }

        findViewById<TextView>(R.id.btn_cancel).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btn_create).setOnClickListener {
            val title = findViewById<EditText>(R.id.input_title).text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            it.isEnabled = false
            (it as TextView).text = "Creating..."

            Thread {
                try {
                    NotionApi.createTask(
                        title = title,
                        phase = selectedPhase,
                        status = selectedStatus,
                        lifeArea = selectedLifeArea,
                        deadline = selectedDeadline,
                        goalId = selectedGoalId
                    )
                    sendBroadcast(Intent(this, QuestWidgetProvider::class.java).apply {
                        action = QuestWidgetProvider.ACTION_REFRESH
                    })
                    runOnUiThread { finish() }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        (it as TextView).text = "Create"
                        it.isEnabled = true
                    }
                }
            }.start()
        }
    }

    private fun showGoalPicker(tv: TextView) {
        val names = listOf("None") + goals.map { it.title }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
            .setTitle("Select Goal")
            .setItems(names.toTypedArray()) { _, which ->
                if (which == 0) {
                    selectedGoalId = null
                    selectedGoalTitle = null
                    tv.text = "Tap to select"
                    tv.setTextColor(0x44FFFFFF)
                } else {
                    val goal = goals[which - 1]
                    selectedGoalId = goal.id
                    selectedGoalTitle = goal.title
                    tv.text = goal.title
                    tv.setTextColor(0xFFFFFFFF.toInt())
                }
            }
            .show()
    }

    private fun setupChips(
        containerId: Int,
        labels: List<String>,
        defaultSelected: String?,
        onSelect: (String) -> Unit
    ) {
        val container = findViewById<LinearLayout>(containerId)
        val chips = mutableListOf<TextView>()

        for (label in labels) {
            val chip = TextView(this).apply {
                text = label
                textSize = 11f
                setTextColor(0xFFFFFFFF.toInt())
                background = if (label == defaultSelected) getDrawable(R.drawable.chip_selected_bg)
                    else getDrawable(R.drawable.chip_bg)
                setPadding(dp(8), dp(4), dp(8), dp(4))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.marginEnd = dp(4)
                layoutParams = params
            }
            chip.setOnClickListener {
                for (c in chips) c.background = getDrawable(R.drawable.chip_bg)
                chip.background = getDrawable(R.drawable.chip_selected_bg)
                onSelect(label)
            }
            chips.add(chip)
            container.addView(chip)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
