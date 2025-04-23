package com.example.todolist

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert
    suspend fun insertTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks ORDER BY isPinned DESC, sortOrder ASC")
    suspend fun getAllTasks(): List<Task>

    @Update suspend fun updateTask(task: Task)

    @Query("SELECT * FROM tasks WHERE time = :time LIMIT 1")
    suspend fun getTaskByTime(time: String): Task?
}