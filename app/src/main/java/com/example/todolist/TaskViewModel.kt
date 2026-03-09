package com.example.todolist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    fun loadResetTime() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getResetTime()
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
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getHistoryByDate(date)
            }
            _history.value = result
        }
    }

    fun loadLast30DaysStats() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getLast30DaysStats()
            }
            _last30DaysStats.value = result
        }
    }

    fun loadTasksByListId(listId: Long) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getTasksByListId(listId)
            }
            _tasks.value = result
        }
    }

    fun loadWeeklyTasksForDay(uid: String, day: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getTasksByWeekday(uid, day)
            }
            _weeklyTasks.value = result
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertTask(task)
            }
            // Optional: reload tasks after adding, or manual update
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateTask(task)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteTask(task)
            }
        }
    }

    fun loadAllLists() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getAllLists()
            }
            _lists.value = result
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

    fun updateLists(vararg lists: Todolist) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateList(*lists)
            }
        }
    }
}
