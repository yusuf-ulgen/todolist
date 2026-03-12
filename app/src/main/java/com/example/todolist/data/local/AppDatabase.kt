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
        entities =
                [
                        Task::class,
                        ResetTime::class,
                        DailyStat::class,
                        TaskHistory::class,
                        NotificationPref::class,
                        Todolist::class],
        version = 13,
        exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun resetTimeDao(): ResetTimeDao
    abstract fun dailyStatDao(): DailyStatDao
    abstract fun taskHistoryDao(): TaskHistoryDao
    abstract fun notificationPrefDao(): NotificationPrefDao
    abstract fun todolistDao(): TodolistDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Migration from 10 to 11: add sortOrder to lists table
        private val MIGRATION_10_11 =
                object : Migration(10, 11) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                                "ALTER TABLE lists ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0"
                        )
                    }
                }

        // Migration from 11 to 12: add resetDay to reset_time table
        private val MIGRATION_11_12 =
                object : Migration(11, 12) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                                "ALTER TABLE reset_time ADD COLUMN resetDay INTEGER NOT NULL DEFAULT 0"
                        )
                    }
                }

        // Migration from 12 to 13: add priority to tasks table
        private val MIGRATION_12_13 =
                object : Migration(12, 13) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                                "ALTER TABLE tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 0"
                        )
                        // Create daily_stats if not exists
                        db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `daily_stats` (`date` TEXT NOT NULL, `completed` INTEGER NOT NULL, `total` INTEGER NOT NULL, PRIMARY KEY(`date`))"
                        )

                        // Create task_history if not exists
                        db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `task_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `content` TEXT NOT NULL, `time` TEXT NOT NULL, `isChecked` INTEGER NOT NULL)"
                        )

                        // Ensure notification_pref exists (safety check)
                        db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `notification_pref` (`id` INTEGER NOT NULL, `kind` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                        )
                    }
                }

        fun getDatabase(context: Context): AppDatabase =
                INSTANCE
                        ?: synchronized(this) {
                            val instance =
                                    Room.databaseBuilder(
                                                    context.applicationContext,
                                                    AppDatabase::class.java,
                                                    "task_database"
                                            )
                                            .addMigrations(
                                                    MIGRATION_10_11,
                                                    MIGRATION_11_12,
                                                    MIGRATION_12_13
                                            )
                                            .fallbackToDestructiveMigration()
                                            .addCallback(
                                                    object : Callback() {
                                                        override fun onCreate(
                                                                db: SupportSQLiteDatabase
                                                        ) {
                                                            super.onCreate(db)
                                                            CoroutineScope(Dispatchers.IO).launch {
                                                                INSTANCE?.todolistDao()
                                                                        ?.insert(
                                                                                Todolist(
                                                                                        name =
                                                                                                "GÜNLÜK/HAFTALIK"
                                                                                )
                                                                        )
                                                            }
                                                        }
                                                    }
                                            )
                                            .build()
                            INSTANCE = instance
                            instance
                        }
    }
}
