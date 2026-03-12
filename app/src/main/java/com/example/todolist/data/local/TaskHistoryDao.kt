package com.example.todolist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TaskHistoryDao {
    @Insert
    suspend fun insertAll(histories: List<TaskHistory>)

    @Query("SELECT * FROM task_history WHERE date = :date")
    suspend fun getByDate(date: String): List<TaskHistory>
}