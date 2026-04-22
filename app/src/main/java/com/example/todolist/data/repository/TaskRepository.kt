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
    suspend fun getAllTasks(uid: String): List<Task> = taskDao.getAllTasks(uid)
    
    suspend fun getTasksByListId(uid: String, listId: Long): List<Task> = taskDao.getTasksByListId(uid, listId)
    
    suspend fun getTasksByWeekday(uid: String, day: String, listId: Long): List<Task> = 
        taskDao.getTasksByWeekday(uid, day, listId)
    
    suspend fun insertTask(task: Task) = taskDao.insertTask(task)
    
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    
    suspend fun updateTasks(vararg tasks: Task) = taskDao.updateTasks(*tasks)
    
    suspend fun getTaskByTimeAndUserId(time: String, userId: String): Task? = 
        taskDao.getTaskByTimeAndUserId(time, userId)

    // To-do Lists
    suspend fun getAllLists(uid: String): List<Todolist> = todolistDao.getAllLists(uid)
    
    suspend fun insertList(list: Todolist): Long = todolistDao.insertList(list)
    
    suspend fun updateList(vararg todoLists: Todolist) = todolistDao.updateList(*todoLists)
    
    suspend fun deleteList(todoList: Todolist) = todolistDao.delete(todoList)

    // Daily Stats
    suspend fun upsertDailyStat(stat: DailyStat) = dailyStatDao.upsert(stat)
    suspend fun getLast30DaysStats(uid: String): List<DailyStat> = dailyStatDao.getLast30Days(uid)

    // Task History
    suspend fun insertHistory(history: List<TaskHistory>) = taskHistoryDao.insertAll(history)
    suspend fun getHistoryByDate(date: String, uid: String): List<TaskHistory> = taskHistoryDao.getByDate(date, uid)

    // Notification Preferences
    suspend fun getNotificationPref(uid: String) = notificationPrefDao.getPref(uid)
    
    suspend fun upsertNotificationPref(pref: NotificationPref) = 
        notificationPrefDao.upsert(pref)

    // Reset Time
    suspend fun getResetTime(uid: String) = resetTimeDao.getResetTime(uid)
    
    suspend fun upsertResetTime(resetTime: ResetTime) = resetTimeDao.upsert(resetTime)
}
