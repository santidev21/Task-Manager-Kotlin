package com.example.taskmanager

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView

/**
 * Custom adapter used to display tasks inside the ListView.
 */
class TaskAdapter(
    private val activity: MainActivity,
    private val tasks: MutableList<Task>
) : ArrayAdapter<Task>(
    activity,
    R.layout.task_item,
    tasks
) {

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {

        val view = convertView ?: LayoutInflater
            .from(context)
            .inflate(R.layout.task_item, parent, false)

        val task = tasks[position]

        val tvTaskName =
            view.findViewById<TextView>(R.id.tvTaskName)

        val tvTaskDescription =
            view.findViewById<TextView>(R.id.tvTaskDescription)

        val cbCompleted =
            view.findViewById<CheckBox>(R.id.cbCompleted)

        val btnDelete =
            view.findViewById<ImageButton>(R.id.btnDelete)

        tvTaskName.text = task.name
        tvTaskDescription.text = task.description

        cbCompleted.setOnCheckedChangeListener(null)
        cbCompleted.isChecked = task.completed

        updateTaskStyle(
            task,
            tvTaskName,
            tvTaskDescription
        )

        cbCompleted.setOnCheckedChangeListener { _, isChecked ->

            task.completed = isChecked

            activity.saveTaskChanges(tasks)

            sortTasks()

            notifyDataSetChanged()
        }

        btnDelete.setOnClickListener {

            tasks.remove(task)

            activity.saveTaskChanges(tasks)

            notifyDataSetChanged()
        }

        return view
    }

    /**
     * Applies strike-through style to completed tasks.
     */
    private fun updateTaskStyle(
        task: Task,
        title: TextView,
        description: TextView
    ) {

        if (task.completed) {

            title.paintFlags =
                title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            description.paintFlags =
                description.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

        } else {

            title.paintFlags =
                title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

            description.paintFlags =
                description.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    /**
     * Moves completed tasks to the bottom of the list.
     */
    private fun sortTasks() {

        tasks.sortBy {
            it.completed
        }
    }
}