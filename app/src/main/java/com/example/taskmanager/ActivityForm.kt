package com.example.taskmanager

import android.os.Bundle
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Screen used to create a new task.
 */
class ActivityForm : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_form)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->

            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        val etTaskName =
            findViewById<TextInputEditText>(R.id.etTaskName)

        val etTaskDescription =
            findViewById<TextInputEditText>(R.id.etTaskDescription)

        val btnSaveTask =
            findViewById<Button>(R.id.btnSaveTask)

        val btnBack =
            findViewById<Button>(R.id.btnBack)

        val taskManager =
            TaskManager(this)

        // Validates and saves the task
        btnSaveTask.setOnClickListener {

            val taskName =
                etTaskName.text.toString().trim()

            val taskDescription =
                etTaskDescription.text.toString().trim()

            if (taskName.isEmpty()) {

                etTaskName.error =
                    getString(R.string.error_task_name_required)

                return@setOnClickListener
            }

            val task = Task(
                name = taskName,
                description = taskDescription
            )

            taskManager.saveTask(task)

            finish()
        }

        // Returns to the previous screen without saving
        btnBack.setOnClickListener {
            finish()
        }
    }
}