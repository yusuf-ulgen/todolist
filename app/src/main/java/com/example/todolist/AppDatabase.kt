package com.example.todolist

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.todolist.data.Todolist
import com.example.todolist.data.TodolistDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Veritabanı ilk defa oluşturulduğunda default listeyi ekle
                            // DAO insert() suspend olduğu için coroutine ile çağırıyoruz
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    database.todolistDao()
                                        .insert(Todolist(name = "GÜNLÜK/HAFTALIK"))
                                }
                            }
                        }
                    })
                    .build()

                INSTANCE = instance
                instance
            }
    }
}