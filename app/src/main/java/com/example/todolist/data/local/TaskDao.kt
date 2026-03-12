package com.example.todolist

import androidx.room.*

@Dao
interface TaskDao {
    @Insert
    suspend fun insertTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Update
    suspend fun updateTasks(vararg tasks: Task)

    @Query(" SELECT * FROM tasks ORDER BY isPinned DESC, sortOrder ASC")
    suspend fun getAllTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE time = :time LIMIT 1")
    suspend fun getTaskByTime(time: String): Task?

    @Query("SELECT * FROM tasks WHERE time = :time AND userId = :userId LIMIT 1")
    suspend fun getTaskByTimeAndUserId(time: String, userId: String): Task?


    @Query("SELECT * FROM tasks WHERE userId = :uid AND weekday = :day AND listId = :listId ORDER BY isPinned DESC, sortOrder ASC")
    suspend fun getTasksByWeekday(uid: String, day: String, listId: Long): List<Task>

    @Query("SELECT * FROM tasks WHERE listId = :listId AND (weekday IS NULL OR weekday = '') ORDER BY isPinned DESC, sortOrder ASC")
    suspend fun getTasksByListId(listId: Long): List<Task>
}