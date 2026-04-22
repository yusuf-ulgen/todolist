package com.example.todolist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_pref", primaryKeys = ["id", "userId"])
data class NotificationPref(
    val id: Int = 0,    // Tek satırlık tablo, sabit PK
    val userId: String, // Firebase UID
    val kind: Int       // 0 = her görev, 1 = yalnız pinlilere, 2 = hiçbiri
)