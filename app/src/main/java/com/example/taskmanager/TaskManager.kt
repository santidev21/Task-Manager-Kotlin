package com.example.taskmanager

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TaskManager {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    // Builds the authenticated database path for the current user's tasks.
    private fun tasksReference(): DatabaseReference {
        val userId = auth.currentUser?.uid
            ?: throw IllegalStateException("User is not authenticated")

        return database.reference
            .child("users")
            .child(userId)
            .child("tasks")
    }

    // Listens to real-time task changes for the active user.
    fun observeTasks(
        onTasksLoaded: (List<Task>) -> Unit,
        onError: (Exception) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<Task>()

                snapshot.children.forEach { child ->
                    val task = child.getValue(Task::class.java)
                    if (task != null) {
                        tasks.add(task.copy(id = child.key.orEmpty()))
                    }
                }

                onTasksLoaded(tasks)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.toException())
            }
        }

        tasksReference().addValueEventListener(listener)
        return listener
    }

    // Detaches a Firebase listener when the screen is no longer visible.
    fun removeTasksListener(listener: ValueEventListener) {
        try {
            tasksReference().removeEventListener(listener)
        } catch (_: Exception) {
        }
    }

    // Saves a new task or reuses the provided id when editing an existing one.
    fun saveTask(
        task: Task,
        onSuccess: (Task) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val reference = tasksReference()
            val taskId = task.id.ifBlank {
                reference.push().key ?: throw IllegalStateException("Unable to generate a task id")
            }

            val taskToSave = task.copy(
                id = taskId,
                createdAt = if (task.createdAt == 0L) System.currentTimeMillis() else task.createdAt
            )

            reference.child(taskId)
                .setValue(taskToSave)
                .addOnSuccessListener { onSuccess(taskToSave) }
                .addOnFailureListener { onError(it as? Exception ?: Exception(it)) }
        } catch (exception: Exception) {
            onError(exception)
        }
    }

    // Replaces the stored task data with the latest local version.
    fun updateTask(
        task: Task,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val taskId = task.id.ifBlank {
                throw IllegalArgumentException("Task id is required")
            }

            tasksReference().child(taskId)
                .setValue(task)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onError(it as? Exception ?: Exception(it)) }
        } catch (exception: Exception) {
            onError(exception)
        }
    }

    // Deletes a task from the current user's Firebase node.
    fun deleteTask(
        taskId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            if (taskId.isBlank()) {
                throw IllegalArgumentException("Task id is required")
            }

            tasksReference().child(taskId)
                .removeValue()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onError(it as? Exception ?: Exception(it)) }
        } catch (exception: Exception) {
            onError(exception)
        }
    }
}
