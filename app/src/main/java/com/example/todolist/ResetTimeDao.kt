package com.example.todolist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ResetTimeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(resetTime: ResetTime)

    @Query("SELECT * FROM reset_time WHERE id = 0 LIMIT 1")
    suspend fun getResetTime(): ResetTime?
}