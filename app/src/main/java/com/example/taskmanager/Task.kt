package com.example.taskmanager

import com.google.firebase.database.Exclude

data class Task(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var completed: Boolean = false,
    var locationName: String = "",
    @get:Exclude var latitude: Double = 0.0,
    @get:Exclude var longitude: Double = 0.0,
    var createdAt: Long = 0L,
    var encryptedCoords: String = ""
)