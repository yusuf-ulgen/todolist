package com.example.todolist

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow


import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.tasks.await

class TaskRepository(
    private val taskDao: TaskDao,
    private val todolistDao: TodolistDao,
    private val dailyStatDao: DailyStatDao,
    private val taskHistoryDao: TaskHistoryDao,
    private val notificationPrefDao: NotificationPrefDao,
    private val resetTimeDao: ResetTimeDao
) {
    private val firestore = FirebaseFirestore.getInstance()
    private var tasksListener: ListenerRegistration? = null
    private var listsListener: ListenerRegistration? = null

    // Tasks
    suspend fun getAllTasks(uid: String): List<Task> = taskDao.getAllTasks(uid)
    
    suspend fun getTasksByListId(uid: String, listId: String): List<Task> = taskDao.getTasksByListId(uid, listId)
    
    suspend fun getTasksByWeekday(uid: String, day: String, listId: String): List<Task> = 
        taskDao.getTasksByWeekday(uid, day, listId)
    
    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
        firestore.collection("users").document(task.userId)
            .collection("tasks").document(task.id).set(task).await()
    }
    
    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
        firestore.collection("users").document(task.userId)
            .collection("tasks").document(task.id).delete().await()
    }
    
    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
        firestore.collection("users").document(task.userId)
            .collection("tasks").document(task.id).set(task).await()
    }
    
    suspend fun updateTasks(vararg tasks: Task) {
        taskDao.updateTasks(*tasks)
        tasks.forEach { task ->
            firestore.collection("users").document(task.userId)
                .collection("tasks").document(task.id).set(task)
        }
    }
    
    suspend fun getTaskByTimeAndUserId(time: String, userId: String): Task? = 
        taskDao.getTaskByTimeAndUserId(time, userId)

    // To-do Lists
    suspend fun getAllLists(uid: String): List<Todolist> = todolistDao.getAllLists(uid)
    
    suspend fun insertList(list: Todolist) {
        todolistDao.insertList(list)
        firestore.collection("users").document(list.userId)
            .collection("lists").document(list.id).set(list).await()
    }
    
    suspend fun updateList(vararg todoLists: Todolist) {
        todolistDao.updateList(*todoLists)
        todoLists.forEach { list ->
            firestore.collection("users").document(list.userId)
                .collection("lists").document(list.id).set(list)
        }
    }
    
    suspend fun deleteList(todoList: Todolist) {
        todolistDao.delete(todoList)
        firestore.collection("users").document(todoList.userId)
            .collection("lists").document(todoList.id).delete().await()
    }

    // Real-time Listeners
    fun startRealtimeSync(uid: String, onTasksChanged: () -> Unit) {
        tasksListener?.remove()
        tasksListener = firestore.collection("users").document(uid)
            .collection("tasks")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                
                snapshots?.documentChanges?.forEach { change ->
                    val task = change.document.toObject(Task::class.java)
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                        when (change.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED,
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> taskDao.insertTask(task) // insertTask in Room acts as upsert if configured, or use a separate upsert
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> taskDao.deleteTask(task)
                        }
                    }
                }
                onTasksChanged()
            }
            
        listsListener?.remove()
        listsListener = firestore.collection("users").document(uid)
            .collection("lists")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                snapshots?.documentChanges?.forEach { change ->
                    val list = change.document.toObject(Todolist::class.java)
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                        when (change.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED,
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> todolistDao.insert(list)
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> todolistDao.delete(list)
                        }
                    }
                }
            }
    }

    fun stopRealtimeSync() {
        tasksListener?.remove()
        listsListener?.remove()
    }

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

    suspend fun migrateDataToFirestore(uid: String) {
        val allTasks = taskDao.getAllTasksUnfiltered()
        val allLists = todolistDao.getAllLists(uid)
        
        allTasks.forEach { task ->
            firestore.collection("users").document(uid)
                .collection("tasks").document(task.id).set(task).await()
        }
        
        allLists.forEach { list ->
            firestore.collection("users").document(uid)
                .collection("lists").document(list.id).set(list).await()
        }
    }
}
