package com.example.todolist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName
import java.util.UUID

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey 
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "userId") 
    @get:PropertyName("userId")
    @set:PropertyName("userId")
    var userId: String = "",

    @ColumnInfo(name = "content") 
    @get:PropertyName("content")
    @set:PropertyName("content")
    var content: String = "",

    @ColumnInfo(name = "time") 
    @get:PropertyName("time")
    @set:PropertyName("time")
    var time: String = "",

    @ColumnInfo(name = "isChecked") 
    @get:PropertyName("isChecked")
    @set:PropertyName("isChecked")
    var isChecked: Boolean = false,

    @ColumnInfo(name = "isPinned") 
    @get:PropertyName("isPinned")
    @set:PropertyName("isPinned")
    var isPinned: Boolean = false,

    @ColumnInfo(name = "sortOrder") 
    @get:PropertyName("sortOrder")
    @set:PropertyName("sortOrder")
    var sortOrder: Int = 0,

    @ColumnInfo(name = "weekday") 
    @get:PropertyName("weekday")
    @set:PropertyName("weekday")
    var weekday: Any? = null,

    @ColumnInfo(name = "listId", defaultValue = "default") 
    @get:PropertyName("listId")
    @set:PropertyName("listId")
    var listId: String = "default",

    @ColumnInfo(name = "priority", defaultValue = "0") 
    @get:PropertyName("priority")
    @set:PropertyName("priority")
    var priority: Int = 0
) {
    // Empty constructor for Firestore
    constructor() : this(UUID.randomUUID().toString(), "", "", "", false, false, 0, null, "default", 0)
}