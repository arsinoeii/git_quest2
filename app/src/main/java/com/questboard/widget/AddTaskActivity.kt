package com.questboard.widget

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
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
    private var goals: List<NotionApi.GoalItem> = emptyList()

    private var statusLabel: TextView? = null
    private var statusContainer: LinearLayout? = null
    private val statusChips = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        findViewById<View>(R.id.overlay_root).setOnClickListener { finish() }
        findViewById<View>(R.id.card).setOnClickListener { /* consume */ }

        statusLabel = findViewById(R.id.label_status)
        statusContainer = findViewById(R.id.chips_status)

        val phaseRow1 = listOf("🏁 This Week", "This Month", "🧠 Dump")
        val phaseRow2 = listOf("This Quarter", "This Year")
        setupChipsMultiRow(
            listOf(R.id.chips_phase, R.id.chips_phase_row2),
            listOf(phaseRow1, phaseRow2),
            selectedPhase
        ) { phase ->
            selectedPhase = phase
            updateStatusForPhase(phase)
        }

        updateStatusForPhase(selectedPhase)

        val lifeAreaRow1 = listOf("🧾 Life Admin", "💼 Career")
        val lifeAreaRow2 = listOf("💬🫶 Relationships", "🧘‍♀️ Health")
        val lifeAreaRow3 = listOf("🎨 Joy", "💸 Finances", "🪷 Home")
        setupChipsMultiRow(
            listOf(R.id.chips_life_area, R.id.chips_life_area_row2, R.id.chips_life_area_row3),
            listOf(lifeAreaRow1, lifeAreaRow2, lifeAreaRow3),
            null
        ) { selectedLifeArea = it }

        findViewById<TextView>(R.id.input_deadline).setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                selectedDeadline = "%04d-%02d-%02d".format(y, m + 1, d)
                (it as TextView).text = selectedDeadline
                it.setTextColor(0xFF3a4830.toInt())
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

            val actualLifeArea = when (selectedLifeArea) {
                "🧘‍♀️ Health" -> "🧘‍♀️ Health & Wellbeing"
                "🎨 Joy" -> "🎨 Joy & Creativity"
                "🪷 Home" -> "🪷 Home & Space"
                else -> selectedLifeArea
            }

            Thread {
                try {
                    NotionApi.createTask(
                        title = title,
                        phase = selectedPhase,
                        status = selectedStatus,
                        lifeArea = actualLifeArea,
                        deadline = selectedDeadline,
                        goalId = selectedGoalId
                    )
                    sendBroadcast(android.content.Intent(this, QuestWidgetProvider::class.java).apply {
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

    private fun updateStatusForPhase(phase: String) {
        val container = statusContainer ?: return
        val label = statusLabel ?: return
        container.removeAllViews()
        statusChips.clear()

        when {
            phase == "🏁 This Week" -> {
                label.visibility = View.VISIBLE
                container.visibility = View.VISIBLE
                val options = listOf("Today", "To Do")
                selectedStatus = "Today"
                for (opt in options) {
                    val chip = createChip(opt, opt == selectedStatus)
                    chip.setOnClickListener {
                        for (c in statusChips) c.background = getDrawable(R.drawable.chip_bg)
                        chip.background = getDrawable(R.drawable.chip_selected_bg)
                        selectedStatus = opt
                    }
                    statusChips.add(chip)
                    container.addView(chip)
                }
            }
            else -> {
                label.visibility = View.VISIBLE
                container.visibility = View.VISIBLE
                selectedStatus = "Not Started"
                val chip = createChip("Not Started", true)
                statusChips.add(chip)
                container.addView(chip)
            }
        }
    }

    private fun showGoalPicker(tv: TextView) {
        val names = listOf("None") + goals.map { it.title }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
            .setTitle("Select Goal")
            .setItems(names.toTypedArray()) { _, which ->
                if (which == 0) {
                    selectedGoalId = null
                    tv.text = "Tap to select"
                    tv.setTextColor(0x886a5a3a.toInt())
                } else {
                    val goal = goals[which - 1]
                    selectedGoalId = goal.id
                    tv.text = goal.title
                    tv.setTextColor(0xFF3a4830.toInt())
                }
            }
            .show()
    }

    private fun setupChipsMultiRow(
        containerIds: List<Int>,
        rowLabels: List<List<String>>,
        defaultSelected: String?,
        onSelect: (String) -> Unit
    ) {
        val allChips = mutableListOf<TextView>()
        for (rowIdx in containerIds.indices) {
            val container = findViewById<LinearLayout>(containerIds[rowIdx])
            val labels = if (rowIdx < rowLabels.size) rowLabels[rowIdx] else emptyList()
            for (label in labels) {
                val chip = createChip(label, label == defaultSelected)
                chip.setOnClickListener {
                    for (c in allChips) c.background = getDrawable(R.drawable.chip_bg)
                    chip.background = getDrawable(R.drawable.chip_selected_bg)
                    onSelect(label)
                }
                allChips.add(chip)
                container.addView(chip)
            }
        }
    }

    private fun createChip(label: String, selected: Boolean): TextView {
        return TextView(this).apply {
            text = label
            textSize = 11f
            setTextColor(0xFF3a4830.toInt())
            background = if (selected) getDrawable(R.drawable.chip_selected_bg)
                else getDrawable(R.drawable.chip_bg)
            setPadding(dp(8), dp(5), dp(8), dp(5))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(4)
                bottomMargin = dp(2)
            }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
