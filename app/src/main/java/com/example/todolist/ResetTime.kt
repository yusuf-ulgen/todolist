package com.example.todolist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reset_time")
data class ResetTime(
    @PrimaryKey val id: Int = 0,        // hep 0
    val resetHour: Int,
    val resetMinute: Int
)