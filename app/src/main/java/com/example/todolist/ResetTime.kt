package com.example.todolist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reset_time")
data class ResetTime(
    @PrimaryKey val id: Int = 0,        // hep 0 olacak
    val resetHour: Int,
    val resetMinute: Int
)