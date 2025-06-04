package com.example.todolist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lists")
data class Todolist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var name: String,
    var sortOrder: Int = 0
)