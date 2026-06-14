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

    // Sets up the dashboard and redirects unauthenticated users to login.
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
            openTaskForm()
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

    // Starts the Firebase listener that keeps the list in sync.
    override fun onStart() {
        super.onStart()

        if (auth.currentUser == null) {
            openAuthScreen()
            return
        }

        observeTasks()
    }

    // Removes the listener to avoid leaks and duplicate updates.
    override fun onStop() {
        removeTasksListener()
        super.onStop()
    }

    // Subscribes to the current user's tasks and refreshes the list on changes.
    private fun observeTasks() {
        removeTasksListener()

        tasksListener = taskManager.observeTasks(
            onTasksLoaded = { tasks -> renderTasks(tasks) },
            onError = { exception -> showMessage(exception.message ?: getString(R.string.error_loading_tasks)) }
        )
    }

    // Safely detaches the active Firebase listener.
    private fun removeTasksListener() {
        tasksListener?.let {
            taskManager.removeTasksListener(it)
            tasksListener = null
        }
    }

    // Sorts the tasks and refreshes the adapter with the latest snapshot.
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
            onEditTask = { task -> openTaskForm(task) },
            onActionError = { message -> showMessage(message) }
        )
    }

    // Opens the form in create or edit mode depending on the provided task.
    private fun openTaskForm(task: Task? = null) {
        val intent = Intent(this, ActivityForm::class.java)

        task?.let {
            intent.putExtra(ActivityForm.EXTRA_TASK_ID, it.id)
            intent.putExtra(ActivityForm.EXTRA_TASK_NAME, it.name)
            intent.putExtra(ActivityForm.EXTRA_TASK_DESCRIPTION, it.description)
            intent.putExtra(ActivityForm.EXTRA_TASK_LOCATION_NAME, it.locationName)
            intent.putExtra(ActivityForm.EXTRA_TASK_LATITUDE, it.latitude)
            intent.putExtra(ActivityForm.EXTRA_TASK_LONGITUDE, it.longitude)
            intent.putExtra(ActivityForm.EXTRA_TASK_COMPLETED, it.completed)
            intent.putExtra(ActivityForm.EXTRA_TASK_CREATED_AT, it.createdAt)
        }

        startActivity(intent)
    }

    // Asks the user to confirm sign-out before clearing the session.
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

    // Sends the user back to the authentication screen.
    private fun openAuthScreen() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }

    // Displays a short message at the bottom of the screen.
    private fun showMessage(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}
