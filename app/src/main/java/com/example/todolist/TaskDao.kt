package com.example.todolist

import androidx.room.*

@Dao
interface TaskDao {

    @Insert
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task) // Görev güncelleme fonksiyonu

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<Task>

    @Delete
    suspend fun deleteTask(task: Task)
}