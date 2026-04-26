package com.questboard.widget

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

class SetupActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, "Add the Quest Widget from your home screen widget picker", Toast.LENGTH_LONG).show()
        finish()
    }
}
