package com.example.todolist.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "userId") var userId: String,                // Firebase UID'si eklendi
    @ColumnInfo(name = "content") var content: String,
    @ColumnInfo(name = "time") var time: String,
    @ColumnInfo(name = "isChecked") var isChecked: Boolean = false,  // Checkbox durumu
    @ColumnInfo(name = "isPinned") var isPinned: Boolean = false,
    @ColumnInfo(name = "sortOrder") var sortOrder: Int = 0,
    @ColumnInfo(name = "weekday") var weekday: String? = null,
    @ColumnInfo(name = "listId", defaultValue = "1") var listId: Long
)