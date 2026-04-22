package com.example.todolist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks

    private val _weeklyTasks = MutableLiveData<List<Task>>()
    val weeklyTasks: LiveData<List<Task>> = _weeklyTasks

    private val _lists = MutableLiveData<List<Todolist>>()
    val lists: LiveData<List<Todolist>> = _lists

    private val _resetTime = MutableLiveData<ResetTime?>()
    val resetTime: LiveData<ResetTime?> = _resetTime

    private val _history = MutableLiveData<List<TaskHistory>>()
    val history: LiveData<List<TaskHistory>> = _history

    private val _last30DaysStats = MutableLiveData<List<DailyStat>>()
    val last30DaysStats: LiveData<List<DailyStat>> = _last30DaysStats

    private fun getCurrentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun loadResetTime() {
        val uid = getCurrentUserId() ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getResetTime(uid)
            }
            _resetTime.value = result
        }
    }

    fun saveResetTime(resetTime: ResetTime) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.upsertResetTime(resetTime)
            }
        }
    }

    fun loadHistoryByDate(date: String) {
        val uid = getCurrentUserId() ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getHistoryByDate(date, uid)
            }
            _history.value = result
        }
    }

    fun loadLast30DaysStats() {
        val uid = getCurrentUserId() ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getLast30DaysStats(uid)
            }
            _last30DaysStats.value = result
        }
    }

    fun upsertDailyStat(stat: DailyStat) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.upsertDailyStat(stat)
            }
        }
    }

    fun loadTasksByListId(listId: Long) {
        val uid = getCurrentUserId() ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getTasksByListId(uid, listId)
            }
            _tasks.value = result
        }
    }

    fun loadWeeklyTasksForDay(uid: String, day: String, listId: Long) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getTasksByWeekday(uid, day, listId)
            }
            _weeklyTasks.value = result
        }
    }

    fun addTask(task: Task, onCompleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertTask(task)
            }
            if (task.weekday.isNullOrBlank()) {
                loadTasksByListId(task.listId)
            } else {
                loadWeeklyTasksForDay(task.userId, task.weekday!!, task.listId)
            }
            onCompleted?.invoke()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateTask(task)
            }
            if (task.weekday.isNullOrBlank()) {
                loadTasksByListId(task.listId)
            } else {
                val uid = task.userId 
                if (uid.isNotEmpty() && !task.weekday.isNullOrEmpty()) {
                    loadWeeklyTasksForDay(uid, task.weekday!!, task.listId)
                }
            }
        }
    }

    fun updateTasks(vararg tasks: Task, onCompleted: (() -> Unit)? = null) {
        if (tasks.isEmpty()) {
            onCompleted?.invoke()
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateTasks(*tasks)
            }
            val first = tasks[0]
            val uid = first.userId
            if (first.weekday.isNullOrBlank()) {
                val result = withContext(Dispatchers.IO) {
                    repository.getTasksByListId(uid, first.listId)
                }
                _tasks.value = result
            } else {
                if (uid.isNotEmpty() && !first.weekday.isNullOrEmpty()) {
                    val result = withContext(Dispatchers.IO) {
                        repository.getTasksByWeekday(uid, first.weekday!!, first.listId)
                    }
                    _weeklyTasks.value = result
                }
            }
            onCompleted?.invoke()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteTask(task)
            }
            if (task.weekday.isNullOrBlank()) {
                loadTasksByListId(task.listId)
            } else {
                val uid = task.userId
                if (uid.isNotEmpty() && !task.weekday.isNullOrEmpty()) {
                    loadWeeklyTasksForDay(uid, task.weekday!!, task.listId)
                }
            }
        }
    }

    fun loadAllLists() {
        val uid = getCurrentUserId() ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getAllLists(uid)
            }
            if (result.isEmpty()) {
                // Varsayılan listeyi oluştur
                withContext(Dispatchers.IO) {
                    repository.insertList(Todolist(userId = uid, name = "GÜNLÜK/HAFTALIK"))
                }
                // Tekrar yükle
                val newResult = withContext(Dispatchers.IO) {
                    repository.getAllLists(uid)
                }
                _lists.value = newResult
            } else {
                _lists.value = result
            }
        }
    }

    fun deleteList(todoList: Todolist) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteList(todoList)
            }
            loadAllLists() // Refresh
        }
    }

    fun updateLists(vararg lists: Todolist, onCompleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateList(*lists)
            }
            onCompleted?.invoke()
        }
    }

    fun insertList(todolist: Todolist) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertList(todolist)
            }
            loadAllLists() // Refresh LiveData
        }
    }
}
