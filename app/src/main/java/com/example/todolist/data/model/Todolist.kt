package com.example.todolist

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "lists")
data class Todolist(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var userId: String,                // Firebase UID'si eklendi
    var name: String,
    var sortOrder: Int = 0
) {
    // Empty constructor for Firestore
    constructor() : this("", "", "", 0)
}