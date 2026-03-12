package com.example.todolist

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.annotation.SuppressLint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = AppDatabase.getDatabase(context)
            val taskDao = db.taskDao()
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            CoroutineScope(Dispatchers.IO).launch {
                val allTasks = taskDao.getAllTasks()
                allTasks.forEach { task ->
                    if (task.time.isNotBlank() && task.time != "Saat") {
                        scheduleTaskNotification(context, am, task)
                    }
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private fun scheduleTaskNotification(context: Context, am: AlarmManager, task: Task) {
        val parts = task.time.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (!task.weekday.isNullOrEmpty()) {
                val targetDow = DayOfWeek.valueOf(task.weekday!!).value % 7 + 1
                set(Calendar.DAY_OF_WEEK, targetDow)
                if (timeInMillis <= System.currentTimeMillis())
                    add(Calendar.WEEK_OF_YEAR, 1)
            } else {
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("taskId", task.id.toInt())
            putExtra("taskContent", task.content)
            putExtra("listId", task.listId)
            putExtra("isPinned", task.isPinned)
        }

        val pi = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } catch (se: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }
}
