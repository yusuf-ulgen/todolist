package com.example.todolist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reset_time", primaryKeys = ["id", "userId"])
data class ResetTime(
    val id: Int = 0,        // hep 0
    val userId: String,     // Firebase UID
    val resetHour: Int,
    val resetMinute: Int,
    val resetDay: Int
)