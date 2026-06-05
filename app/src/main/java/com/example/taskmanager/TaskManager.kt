package com.example.taskmanager

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Handles task storage using SharedPreferences.
 *
 * Tasks are stored as JSON to allow saving multiple
 * Task objects in local storage.
 */
class TaskManager(
    private val context: Context
) {

    private val sharedPreferences =
        context.getSharedPreferences("Tasks", Context.MODE_PRIVATE)

    private val gson = Gson()

    /**
     * Saves a new task in local storage.
     */
    fun saveTask(task: Task) {

        val tasks = getTasks().toMutableList()

        tasks.add(task)

        val json = gson.toJson(tasks)

        sharedPreferences
            .edit()
            .putString("tasks", json)
            .apply()
    }

    /**
     * Saves the complete task list in local storage.
     */
    fun saveTasks(tasks: List<Task>) {

        val json = gson.toJson(tasks)

        sharedPreferences
            .edit()
            .putString("tasks", json)
            .apply()
    }

    /**
     * Returns all saved tasks.
     */
    fun getTasks(): List<Task> {

        val json =
            sharedPreferences.getString("tasks", null)

        if (json.isNullOrEmpty()) {
            return emptyList()
        }

        val type =
            object : TypeToken<List<Task>>() {}.type

        return gson.fromJson(json, type)
    }

    /**
     * Deletes a task from local storage.
     */
    fun deleteTask(task: Task) {

        val tasks =
            getTasks().toMutableList()

        tasks.remove(task)

        val json =
            gson.toJson(tasks)

        sharedPreferences
            .edit()
            .putString("tasks", json)
            .apply()
    }
}