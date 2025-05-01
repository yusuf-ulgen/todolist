package com.example.todolist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Bu receiver alarm tetiklendiğinde çalışacak
        val db = AppDatabase.getDatabase(context)
        val taskDao = db.taskDao()
        val dailyStatDao = db.dailyStatDao()
        val taskHistoryDao = db.taskHistoryDao()

        CoroutineScope(Dispatchers.IO).launch {
            val all = taskDao.getAllTasks()
            val completed = all.count { it.isChecked }
            val total     = all.size

            // Bugünün tarihi
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date())

            // 1) Günlük istatistiği upsert et (varsa replace, yoksa insert)
            dailyStatDao.upsert(DailyStat(todayKey, completed, total))

            // 2) TaskHistory’yi doldur
            val history = all.map { t ->
                TaskHistory(
                    date      = todayKey,
                    content   = t.content,
                    time      = t.time,
                    isChecked = t.isChecked
                )
            }
            taskHistoryDao.insertAll(history)

            // 3) Task’ları işaretleri kaldırarak güncelle
            all.forEach {
                it.isChecked = false
                taskDao.updateTask(it)
            }

            // 4) Bir kere çalışması için SharedPreferences’a flag yaz (opsiyonel)
            context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit()
                .putString("last_reset_day", todayKey)
                .apply()
        }
    }
}
