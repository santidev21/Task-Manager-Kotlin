package com.example.taskmanager

data class Task(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var completed: Boolean = false,
    var locationName: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var createdAt: Long = 0L
)
