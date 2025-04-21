package com.example.todolist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "content") var content: String,
    @ColumnInfo(name = "time") var time: String,
    @ColumnInfo(name = "isChecked") var isChecked: Boolean = false, // Checkbox durumu
    @ColumnInfo(name = "isPinned") var isPinned: Boolean = false
)