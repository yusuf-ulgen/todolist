package com.example.todolist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats", primaryKeys = ["date", "userId"])
data class DailyStat(
    val date: String,       // yyyy-MM-dd
    val userId: String,     // Firebase UID
    val completed: Int,
    val total: Int
)