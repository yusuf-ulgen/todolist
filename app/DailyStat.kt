package com.example.todolist

// DailyStat.kt
@Entity(tableName = "daily_stats")
data class DailyStat(
    @PrimaryKey val date: String,      // "yyyy-MM-dd" formatında
    @ColumnInfo val completed: Int,    // O gün tamamlanan görev sayısı
    @ColumnInfo val total: Int         // O gün toplam görev sayısı
)

// DailyStatDao.kt
@Dao
interface DailyStatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: DailyStat)

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT 30")
    suspend fun getLast30Days(): List<DailyStat>

    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyStat?
}
