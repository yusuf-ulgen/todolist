package com.example.todolist

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.todolist.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Task::class, ResetTime::class, DailyStat::class, TaskHistory::class, NotificationPref::class, Todolist::class], version = 11, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun resetTimeDao(): ResetTimeDao
    abstract fun dailyStatDao(): DailyStatDao
    abstract fun taskHistoryDao(): TaskHistoryDao
    abstract fun notificationPrefDao(): NotificationPrefDao
    abstract fun todolistDao(): TodolistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE lists ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_database"
                )
                    .addMigrations(MIGRATION_10_11)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.todolistDao()
                                    ?.insert(Todolist(name = "GÜNLÜK/HAFTALIK"))
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
    }
}