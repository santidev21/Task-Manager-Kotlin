package com.example.taskmanager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val taskManager by lazy { TaskManager() }

    private var tasksListener: ValueEventListener? = null

    private lateinit var listTasks: ListView
    private lateinit var tvEmptyState: TextView
    private lateinit var tvUserEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            openAuthScreen()
            return
        }

        setContentView(R.layout.activity_main)

        listTasks = findViewById(R.id.listTasks)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvUserEmail = findViewById(R.id.tvUserEmail)

        findViewById<Button>(R.id.btnAddTask).setOnClickListener {
            startActivity(Intent(this, ActivityForm::class.java))
        }

        findViewById<Button>(R.id.btnOpenMap).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            showLogoutConfirmation()
        }

        tvUserEmail.text = auth.currentUser?.email?.let {
            getString(R.string.main_signed_in_as, it)
        } ?: getString(R.string.main_signed_in_as_guest)
    }

    override fun onStart() {
        super.onStart()

        if (auth.currentUser == null) {
            openAuthScreen()
            return
        }

        observeTasks()
    }

    override fun onStop() {
        removeTasksListener()
        super.onStop()
    }

    private fun observeTasks() {
        removeTasksListener()

        tasksListener = taskManager.observeTasks(
            onTasksLoaded = { tasks -> renderTasks(tasks) },
            onError = { exception -> showMessage(exception.message ?: getString(R.string.error_loading_tasks)) }
        )
    }

    private fun removeTasksListener() {
        tasksListener?.let {
            taskManager.removeTasksListener(it)
            tasksListener = null
        }
    }

    private fun renderTasks(tasks: List<Task>) {
        val sortedTasks = tasks.sortedWith(
            compareBy<Task> { it.completed }.thenByDescending { it.createdAt }
        ).toMutableList()

        if (sortedTasks.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            listTasks.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            listTasks.visibility = View.VISIBLE
        }

        listTasks.adapter = TaskAdapter(
            activity = this,
            tasks = sortedTasks,
            taskManager = taskManager,
            onActionError = { message -> showMessage(message) }
        )
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout_title)
            .setMessage(R.string.logout_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.logout_confirm) { _, _ ->
                removeTasksListener()
                auth.signOut()
                openAuthScreen()
            }
            .show()
    }

    private fun openAuthScreen() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }

    private fun showMessage(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}
