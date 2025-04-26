package com.example.todolist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ResetReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        GlobalScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(ctx)
            val tasks = db.taskDao().getAllTasks()
            tasks.forEach { t ->
                t.isChecked = false
                db.taskDao().updateTask(t)
            }
        }
    }
}