package com.example.taskmanager

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TaskAdapter(
    private val activity: AppCompatActivity,
    private val tasks: MutableList<Task>,
    private val taskManager: TaskManager,
    private val onEditTask: (Task) -> Unit,
    private val onActionError: (String) -> Unit
) : ArrayAdapter<Task>(activity, R.layout.task_item, tasks) {

    // Binds each row and wires the completion, edit, and delete actions.
    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.task_item, parent, false)

        val task = tasks[position]

        val tvTaskName = view.findViewById<TextView>(R.id.tvTaskName)
        val tvTaskDescription = view.findViewById<TextView>(R.id.tvTaskDescription)
        val tvTaskLocation = view.findViewById<TextView>(R.id.tvTaskLocation)
        val cbCompleted = view.findViewById<CheckBox>(R.id.cbCompleted)
        val btnEdit = view.findViewById<ImageButton>(R.id.btnEdit)
        val btnDelete = view.findViewById<ImageButton>(R.id.btnDelete)

        tvTaskName.text = task.name
        tvTaskDescription.text = task.description

        if (task.locationName.isBlank()) {
            tvTaskLocation.visibility = View.GONE
        } else {
            tvTaskLocation.visibility = View.VISIBLE
            tvTaskLocation.text = task.locationName
        }

        cbCompleted.setOnCheckedChangeListener(null)
        cbCompleted.isChecked = task.completed
        updateTaskStyle(task, tvTaskName, tvTaskDescription, tvTaskLocation)

        cbCompleted.setOnCheckedChangeListener { buttonView, isChecked ->
            if (task.completed == isChecked) {
                return@setOnCheckedChangeListener
            }

            val previousState = task.completed
            task.completed = isChecked

            taskManager.updateTask(
                task = task,
                onSuccess = {
                    sortTasks()
                    notifyDataSetChanged()
                },
                onError = { exception ->
                    task.completed = previousState
                    buttonView.isChecked = previousState
                    notifyDataSetChanged()
                    onActionError(exception.message ?: activity.getString(R.string.error_updating_task))
                }
            )
        }

        btnEdit.setOnClickListener {
            onEditTask(task)
        }

        btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.delete_task_title)
                .setMessage(R.string.delete_task_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    taskManager.deleteTask(
                        taskId = task.id,
                        onSuccess = {
                            tasks.remove(task)
                            notifyDataSetChanged()
                        },
                        onError = { exception ->
                            onActionError(exception.message ?: activity.getString(R.string.error_deleting_task))
                        }
                    )
                }
                .show()
        }

        return view
    }

    // Applies strike-through styling when a task is completed.
    private fun updateTaskStyle(
        task: Task,
        title: TextView,
        description: TextView,
        location: TextView
    ) {
        val strikeThrough = Paint.STRIKE_THRU_TEXT_FLAG

        if (task.completed) {
            title.paintFlags = title.paintFlags or strikeThrough
            description.paintFlags = description.paintFlags or strikeThrough
            location.paintFlags = location.paintFlags or strikeThrough
        } else {
            title.paintFlags = title.paintFlags and strikeThrough.inv()
            description.paintFlags = description.paintFlags and strikeThrough.inv()
            location.paintFlags = location.paintFlags and strikeThrough.inv()
        }
    }

    // Keeps unfinished tasks above completed ones while preserving recency.
    private fun sortTasks() {
        tasks.sortWith(compareBy<Task> { it.completed }.thenByDescending { it.createdAt })
    }
}
