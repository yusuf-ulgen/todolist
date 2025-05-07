package com.example.todolist.data

import androidx.room.*

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

    @Query("SELECT * FROM tasks WHERE time != '' AND time != 'Saat'")
    suspend fun getAllTimedTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY isPinned DESC, sortOrder ASC")
    suspend fun getTasksByUserId(userId: String): List<Task>

    @Query("SELECT * FROM tasks WHERE time = :time AND userId = :userId LIMIT 1")
    suspend fun getTaskByTimeAndUserId(time: String, userId: String): Task?

    @Query("SELECT * FROM tasks WHERE userId = :uid AND weekday = :day")
    fun getTasksByWeekday(uid: String, day: String): List<Task>

    @Query("SELECT * FROM tasks WHERE listId = :listId")
    fun getTasksByListId(listId: Long): List<Task>  // listId'ye göre görevleri döndürür
}