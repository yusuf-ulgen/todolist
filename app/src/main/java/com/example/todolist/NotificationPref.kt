package com.example.todolist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_pref")
data class NotificationPref(
    @PrimaryKey val id: Int = 0,    // Tek satırlık tablo, sabit PK
    val kind: Int                   // 0 = her görev, 1 = yalnız pinlilere, 2 = hiçbiri
)