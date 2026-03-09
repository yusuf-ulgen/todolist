package com.example.todolist

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow
import com.example.todolist.data.*

class TaskRepository(
    private val taskDao: TaskDao,
    private val todolistDao: TodolistDao,
    private val dailyStatDao: DailyStatDao,
    private val taskHistoryDao: TaskHistoryDao,
    private val notificationPrefDao: NotificationPrefDao,
    private val resetTimeDao: ResetTimeDao
) {
    // Tasks
    suspend fun getAllTasks(): List<Task> = taskDao.getAllTasks()
    
    suspend fun getTasksByListId(listId: Long): List<Task> = taskDao.getTasksByListId(listId)
    
    suspend fun getTasksByWeekday(uid: String, day: String): List<Task> = 
        taskDao.getTasksByWeekday(uid, day)
    
    suspend fun insertTask(task: Task) = taskDao.insertTask(task)
    
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    
    suspend fun getTaskByTimeAndUserId(time: String, userId: String): Task? = 
        taskDao.getTaskByTimeAndUserId(time, userId)

    // To-do Lists
    suspend fun getAllLists(): List<Todolist> = todolistDao.getAllLists()
    
    suspend fun insertList(list: Todolist): Long = todolistDao.insertList(list)
    
    suspend fun updateList(vararg todoLists: Todolist) = todolistDao.updateList(*todoLists)
    
    suspend fun deleteList(todoList: Todolist) = todolistDao.delete(todoList)

    // Daily Stats
    suspend fun upsertDailyStat(stat: DailyStat) = dailyStatDao.upsert(stat)
    suspend fun getLast30DaysStats(): List<DailyStat> = dailyStatDao.getLast30Days()

    // Task History
    suspend fun insertHistory(history: List<TaskHistory>) = taskHistoryDao.insertAll(history)
    suspend fun getHistoryByDate(date: String): List<TaskHistory> = taskHistoryDao.getByDate(date)

    // Notification Preferences
    suspend fun getNotificationPref() = notificationPrefDao.getPref()
    
    suspend fun upsertNotificationPref(pref: NotificationPref) = 
        notificationPrefDao.upsert(pref)

    // Reset Time
    suspend fun getResetTime() = resetTimeDao.getResetTime()
    
    suspend fun upsertResetTime(resetTime: ResetTime) = resetTimeDao.upsert(resetTime)
}
