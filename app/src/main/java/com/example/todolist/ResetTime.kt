package com.example.todolist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reset_time")
data class ResetTime(
    @PrimaryKey(autoGenerate = true) val id: Int = 1, // Tek bir kayıt olacağı için id 1 olacak
    @ColumnInfo(name = "reset_hour") var resetHour: Int, // Resetleme saati saat kısmı
    @ColumnInfo(name = "reset_minute") var resetMinute: Int // Resetleme saati dakika kısmı
)