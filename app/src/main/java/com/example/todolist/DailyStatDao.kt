package com.example.todolist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DailyStatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: DailyStat)

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT 30")
    suspend fun getLast30Days(): List<DailyStat>

    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyStat?
}
