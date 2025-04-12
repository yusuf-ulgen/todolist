package com.example.todolist

data class Task(
    var content: String,
    var time: String = "Saat",
    var isPinned: Boolean = false,
    var isChecked: Boolean = false
)