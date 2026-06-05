package com.example.taskmanager

/**
 * Represents a task created by the user.
 *
 * A data class was chosen because this object only stores data
 */
data class Task(
    val name: String,
    val description: String,
    var completed: Boolean = false
)