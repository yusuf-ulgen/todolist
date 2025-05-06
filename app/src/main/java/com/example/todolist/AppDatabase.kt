package com.example.todolist

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.todolist.data.Todolist
import com.example.todolist.data.TodolistDao

@Database(
    entities = [Task::class, ResetTime::class, DailyStat::class, TaskHistory::class, NotificationPref::class, Todolist::class], version = 10, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun resetTimeDao(): ResetTimeDao
    abstract fun dailyStatDao(): DailyStatDao
    abstract fun taskHistoryDao(): TaskHistoryDao
    abstract fun notificationPrefDao(): NotificationPrefDao
    abstract fun todolistDao(): TodolistDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_database"
                )
                    .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
          CREATE TABLE IF NOT EXISTS notification_pref (
            id INTEGER PRIMARY KEY NOT NULL,
            kind INTEGER NOT NULL
          )
        """.trimIndent())
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // DEFAULT kaldırıldı, sadece TEXT sütun eklenecek
                db.execSQL("""
          ALTER TABLE tasks
          ADD COLUMN weekday TEXT
        """.trimIndent())
            }
        }

        // AppDatabase companion object içindeki Migration(9,10)
        // AppDatabase.kt içinde
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1) lists tablosunu oluştur (eğer yoksa)
                db.execSQL("""
      CREATE TABLE IF NOT EXISTS lists (
        id   INTEGER PRIMARY KEY NOT NULL,
        name TEXT    NOT NULL
      )
    """.trimIndent())

                // 2) tasks tablosuna listId sütununu NOT NULL, DEFAULT 1 ile ekle
                db.execSQL("""
      ALTER TABLE tasks
      ADD COLUMN listId INTEGER NOT NULL DEFAULT 1
    """.trimIndent())

                // 3) “Varsayılan Liste” yerine “Görevlerim” olarak ekle
                db.execSQL("""
      INSERT OR IGNORE INTO lists (id, name)
      VALUES (1, 'Görevlerim')
    """.trimIndent())
            }
        }
    }
}