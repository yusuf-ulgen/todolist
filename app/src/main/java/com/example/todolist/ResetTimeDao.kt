package com.example.todolist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ResetTimeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResetTime(resetTime: ResetTime)

    @Query("SELECT * FROM reset_time LIMIT 1")
    suspend fun getResetTime(): ResetTime
}