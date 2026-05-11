package com.example.todolist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "userId") var userId: String,                // Firebase UID'si eklendi
    @ColumnInfo(name = "content") var content: String,
    @ColumnInfo(name = "time") var time: String,
    @ColumnInfo(name = "isChecked") var isChecked: Boolean = false,  // Checkbox durumu
    @ColumnInfo(name = "isPinned") var isPinned: Boolean = false,
    @ColumnInfo(name = "sortOrder") var sortOrder: Int = 0,
    @ColumnInfo(name = "weekday") var weekday: String? = null,
    @ColumnInfo(name = "listId", defaultValue = "default") var listId: String = "default",
    @ColumnInfo(name = "priority", defaultValue = "0") var priority: Int = 0
) {
    // Empty constructor for Firestore
    constructor() : this("", "", "", "", false, false, 0, null, "default", 0)
}