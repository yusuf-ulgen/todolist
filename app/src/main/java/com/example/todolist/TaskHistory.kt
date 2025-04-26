package com.example.todolist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_history")
data class TaskHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo val date: String,
    @ColumnInfo val content: String,
    @ColumnInfo val time: String,
    @ColumnInfo(name = "isChecked") val isChecked: Boolean
)
