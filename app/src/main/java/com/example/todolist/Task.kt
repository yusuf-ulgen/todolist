package com.example.todolist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,  // id otomatik olarak artacak
    @ColumnInfo(name = "content") var content: String,  // Görev içeriği
    @ColumnInfo(name = "time") var time: String,  // Görev saati
    @ColumnInfo(name = "is_checked") var isChecked: Boolean = false,  // Görev tamamlandı mı? Checkbox durumu
    @ColumnInfo(name = "is_pinned") var isPinned: Boolean = false  // Sabitlenmiş mi?
)

