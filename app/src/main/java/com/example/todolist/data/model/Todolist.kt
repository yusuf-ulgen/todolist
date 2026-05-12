package com.example.todolist

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName
import java.util.UUID

@Entity(tableName = "lists")
data class Todolist(
    @PrimaryKey 
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = UUID.randomUUID().toString(),
    
    @get:PropertyName("userId")
    @set:PropertyName("userId")
    var userId: String = "",
    
    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",
    
    @get:PropertyName("sortOrder")
    @set:PropertyName("sortOrder")
    var sortOrder: Int = 0
) {
    // Empty constructor for Firestore
    constructor() : this(UUID.randomUUID().toString(), "", "", 0)
}