package com.example.todolist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStat(
    @PrimaryKey val date: String,
    @ColumnInfo val completed: Int,
    @ColumnInfo val total: Int
)
