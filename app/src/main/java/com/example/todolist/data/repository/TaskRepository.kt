package com.example.todolist

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow


import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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
        // 1) O listeye ait tüm görevleri Firestore'dan toplu sil (Batch)
        val tasksQuery = firestore.collection("users").document(todoList.userId)
            .collection("tasks")
            .whereEqualTo("listId", todoList.id)
            .get().await()

        val batch = firestore.batch()
        tasksQuery.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.delete(firestore.collection("users").document(todoList.userId)
            .collection("lists").document(todoList.id))
        
        batch.commit().await()

        // 2) Room'dan görevleri ve listeyi sil
        taskDao.deleteTasksByListId(todoList.userId, todoList.id)
        todolistDao.delete(todoList)
    }

    // Real-time Listeners
    fun startRealtimeSync(uid: String, onTasksChanged: () -> Unit, onListsChanged: () -> Unit) {
        tasksListener?.remove()
        tasksListener = firestore.collection("users").document(uid)
            .collection("tasks")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                
                snapshots?.documentChanges?.forEach { change ->
                    val task = change.document.toObject(Task::class.java)
                    task.id = change.document.id 
                    
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                        when (change.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED,
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> taskDao.insertTask(task)
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> taskDao.deleteTask(task)
                        }
                        withContext(Dispatchers.Main) {
                            onTasksChanged()
                        }
                    }
                }
            }
            
        listsListener?.remove()
        listsListener = firestore.collection("users").document(uid)
            .collection("lists")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                snapshots?.documentChanges?.forEach { change ->
                    val list = change.document.toObject(Todolist::class.java)
                    list.id = change.document.id
                    
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                        when (change.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED,
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> todolistDao.insertList(list)
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> todolistDao.delete(list)
                        }
                        withContext(Dispatchers.Main) {
                            onListsChanged()
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
        
        val batch = firestore.batch()
        var operationCount = 0

        allTasks.forEach { task ->
            if (task.id.isNotBlank()) {
                val ref = firestore.collection("users").document(uid)
                    .collection("tasks").document(task.id)
                batch.set(ref, task)
                operationCount++
                if (operationCount >= 400) {
                    batch.commit().await()
                    operationCount = 0
                }
            }
        }
        
        allLists.forEach { list ->
            if (list.id.isNotBlank()) {
                val ref = firestore.collection("users").document(uid)
                    .collection("lists").document(list.id)
                batch.set(ref, list)
                operationCount++
                if (operationCount >= 400) {
                    batch.commit().await()
                    operationCount = 0
                }
            }
        }

        if (operationCount > 0) {
            batch.commit().await()
        }
    }
}
