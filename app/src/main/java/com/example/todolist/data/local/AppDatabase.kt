package com.example.todolist

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
    
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.todolist.data.local.Converters

@Database(
        entities =
                [
                        Task::class,
                        ResetTime::class,
                        DailyStat::class,
                        TaskHistory::class,
                        NotificationPref::class,
                        Todolist::class],
        version = 15,
        exportSchema = false
)
@TypeConverters(Converters::class)
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

        // Migration from 13 to 14: Add userId columns to all tables
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val tables = listOf("tasks", "lists", "daily_stats", "task_history", "notification_pref", "reset_time")
                tables.forEach { table ->
                    val cursor = db.query("PRAGMA table_info($table)")
                    var hasUserId = false
                    while (cursor.moveToNext()) {
                        if (cursor.getString(1) == "userId") hasUserId = true
                    }
                    cursor.close()
                    if (!hasUserId) {
                        db.execSQL("ALTER TABLE $table ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                    }
                }
            }
        }

        // Migration from 14 to 15: Finalize schemas (ID types and Primary Keys)
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Tasks - Check if listId and priority exist first
                val tasksInfo = db.query("PRAGMA table_info(tasks)")
                val columns = mutableListOf<String>()
                while (tasksInfo.moveToNext()) { columns.add(tasksInfo.getString(1)) }
                tasksInfo.close()

                val hasListId = columns.contains("listId")
                val hasPriority = columns.contains("priority")
                val hasIsPinned = columns.contains("isPinned")
                val hasSortOrder = columns.contains("sortOrder")

                db.execSQL("CREATE TABLE IF NOT EXISTS tasks_new (id TEXT NOT NULL, userId TEXT NOT NULL, content TEXT NOT NULL, time TEXT NOT NULL, isChecked INTEGER NOT NULL, isPinned INTEGER NOT NULL, sortOrder INTEGER NOT NULL, weekday TEXT, listId TEXT NOT NULL DEFAULT 'default', priority INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))")
                
                val selectFields = mutableListOf("CAST(id AS TEXT)", "userId", "content", "time", "isChecked")
                selectFields.add(if (hasIsPinned) "isPinned" else "0")
                selectFields.add(if (hasSortOrder) "sortOrder" else "0")
                selectFields.add("weekday")
                selectFields.add(if (hasListId) "CAST(listId AS TEXT)" else "'default'")
                selectFields.add(if (hasPriority) "priority" else "0")

                db.execSQL("INSERT INTO tasks_new (id, userId, content, time, isChecked, isPinned, sortOrder, weekday, listId, priority) SELECT ${selectFields.joinToString(", ")} FROM tasks")
                db.execSQL("DROP TABLE tasks")
                db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")

                // 2. Lists
                db.execSQL("CREATE TABLE IF NOT EXISTS lists_new (id TEXT NOT NULL, userId TEXT NOT NULL, name TEXT NOT NULL, sortOrder INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("INSERT INTO lists_new (id, userId, name, sortOrder) SELECT CAST(id AS TEXT), userId, name, sortOrder FROM lists")
                db.execSQL("DROP TABLE lists")
                db.execSQL("ALTER TABLE lists_new RENAME TO lists")

                // 3. Daily Stats (PK: date, userId)
                db.execSQL("CREATE TABLE IF NOT EXISTS daily_stats_new (date TEXT NOT NULL, userId TEXT NOT NULL, completed INTEGER NOT NULL, total INTEGER NOT NULL, PRIMARY KEY(date, userId))")
                db.execSQL("INSERT INTO daily_stats_new (date, userId, completed, total) SELECT date, userId, completed, total FROM daily_stats")
                db.execSQL("DROP TABLE daily_stats")
                db.execSQL("ALTER TABLE daily_stats_new RENAME TO daily_stats")

                // 4. Task History (Add userId)
                db.execSQL("CREATE TABLE IF NOT EXISTS task_history_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId TEXT NOT NULL, date TEXT NOT NULL, content TEXT NOT NULL, time TEXT NOT NULL, isChecked INTEGER NOT NULL)")
                db.execSQL("INSERT INTO task_history_new (userId, date, content, time, isChecked) SELECT userId, date, content, time, isChecked FROM task_history")
                db.execSQL("DROP TABLE task_history")
                db.execSQL("ALTER TABLE task_history_new RENAME TO task_history")

                // 5. Notification Pref (PK: id, userId)
                db.execSQL("CREATE TABLE IF NOT EXISTS notification_pref_new (id INTEGER NOT NULL, userId TEXT NOT NULL, kind INTEGER NOT NULL, PRIMARY KEY(id, userId))")
                db.execSQL("INSERT INTO notification_pref_new (id, userId, kind) SELECT id, userId, kind FROM notification_pref")
                db.execSQL("DROP TABLE notification_pref")
                db.execSQL("ALTER TABLE notification_pref_new RENAME TO notification_pref")

                // 6. Reset Time (PK: id, userId)
                db.execSQL("CREATE TABLE IF NOT EXISTS reset_time_new (id INTEGER NOT NULL, userId TEXT NOT NULL, resetHour INTEGER NOT NULL, resetMinute INTEGER NOT NULL, resetDay INTEGER NOT NULL, PRIMARY KEY(id, userId))")
                db.execSQL("INSERT INTO reset_time_new (id, userId, resetHour, resetMinute, resetDay) SELECT id, userId, resetHour, resetMinute, resetDay FROM reset_time")
                db.execSQL("DROP TABLE reset_time")
                db.execSQL("ALTER TABLE reset_time_new RENAME TO reset_time")
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
                                                    MIGRATION_12_13,
                                                    MIGRATION_13_14,
                                                    MIGRATION_14_15
                                            )
                                            .fallbackToDestructiveMigration()
                                            .build()
                            INSTANCE = instance
                            instance
                        }
    }
}
