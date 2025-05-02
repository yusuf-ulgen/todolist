package com.example.todolist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Task::class, ResetTime::class, DailyStat::class, TaskHistory::class, NotificationPref::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun resetTimeDao(): ResetTimeDao
    abstract fun dailyStatDao(): DailyStatDao
    abstract fun taskHistoryDao(): TaskHistoryDao
    abstract fun notificationPrefDao(): NotificationPrefDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_database"
                )
                    // Var olan migration’ları ekleyin:
                    .addMigrations(MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Mevcut 7→8 migration
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notification_pref (
                      id INTEGER PRIMARY KEY NOT NULL,
                      kind INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Yeni 8→9 migration: Task tablosuna weekday sütunu ekliyoruz
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Sadece DEFAULT at, NOT NULL kaldır
                db.execSQL(
                    """
        ALTER TABLE tasks
        ADD COLUMN weekday TEXT DEFAULT ''
      """.trimIndent()
                )
            }
        }
    }
}