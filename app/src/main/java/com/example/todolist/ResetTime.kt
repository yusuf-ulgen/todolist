package com.example.todolist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reset_time")
data class ResetTime(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val resetHour: Int,
    val resetMinute: Int
)