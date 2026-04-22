package com.example.todolist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DailyStatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: DailyStat)

    @Query("SELECT * FROM daily_stats WHERE userId = :uid ORDER BY date DESC LIMIT 30")
    suspend fun getLast30Days(uid: String): List<DailyStat>

    @Query("SELECT * FROM daily_stats WHERE date = :date AND userId = :uid LIMIT 1")
    suspend fun getByDate(date: String, uid: String): DailyStat?
}