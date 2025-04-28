package com.example.todolist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Task::class, ResetTime::class, DailyStat::class, TaskHistory::class],version = 5,exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun resetTimeDao(): ResetTimeDao
    abstract fun dailyStatDao(): DailyStatDao
    abstract fun taskHistoryDao(): TaskHistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_database"
                )
                    // Gelişim aşamasında hızlıca:
                    .fallbackToDestructiveMigration()
                    //.addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // 2→3: sortOrder + daily_stats tablosunu ekler
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                  CREATE TABLE IF NOT EXISTS daily_stats (
                    date TEXT PRIMARY KEY NOT NULL,
                    completed INTEGER NOT NULL,
                    total INTEGER NOT NULL
                  )
                """.trimIndent())
            }
        }

        // 3→4: task_history tablosunu ekler
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                  CREATE TABLE IF NOT EXISTS task_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    date TEXT NOT NULL,
                    content TEXT NOT NULL,
                    time TEXT NOT NULL,
                    isChecked INTEGER NOT NULL
                  )
                """.trimIndent())
            }
        }
    }
}