package com.example.taskmanager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Main screen that displays the task list.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var listTasks: ListView

    private lateinit var tvEmptyState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

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

        listTasks =
            findViewById(R.id.listTasks)

        tvEmptyState =
            findViewById(R.id.tvEmptyState)

        val btnAddTask =
            findViewById<Button>(R.id.btnAddTask)

        btnAddTask.setOnClickListener {

            val intent =
                Intent(this, ActivityForm::class.java)

            startActivity(intent)
        }
    }

    /**
     * Reload tasks every time the screen becomes visible.
     */
    override fun onResume() {
        super.onResume()

        loadTasks()
    }

    /**
     * Loads all tasks from local storage and displays them.
     */
    private fun loadTasks() {

        val taskManager =
            TaskManager(this)

        val tasks =
            taskManager.getTasks()
                .sortedBy { it.completed }
                .toMutableList()

        if (tasks.isEmpty()) {

            tvEmptyState.visibility =
                View.VISIBLE

            listTasks.visibility =
                View.GONE

        } else {

            tvEmptyState.visibility =
                View.GONE

            listTasks.visibility =
                View.VISIBLE
        }

        val adapter =
            TaskAdapter(
                this,
                tasks
            )

        listTasks.adapter = adapter
    }

    /**
     * Saves changes made from the adapter.
     */
    fun saveTaskChanges(tasks: List<Task>) {

        val taskManager =
            TaskManager(this)

        taskManager.saveTasks(tasks)
    }
}