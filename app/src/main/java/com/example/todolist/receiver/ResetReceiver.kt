package com.example.todolist

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
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
        // Alarm tetiklendiğinde hem günlük hem haftalık (o gün) görevleri resetle
        val db = AppDatabase.getDatabase(context)
        val taskDao = db.taskDao()
        val dailyStatDao = db.dailyStatDao()
        val taskHistoryDao = db.taskHistoryDao()
        val resetTimeDao = db.resetTimeDao()

        CoroutineScope(Dispatchers.IO).launch {
            val allTasks = taskDao.getAllTasks()
            val completed = allTasks.count { it.isChecked }
            val total = allTasks.size
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            dailyStatDao.upsert(DailyStat(todayKey, completed, total))

            // 3) History kaydet
            val history = allTasks.map { t ->
                TaskHistory(
                    date = todayKey,
                    content = t.content,
                    time = t.time,
                    isChecked = t.isChecked
                )
            }
            taskHistoryDao.insertAll(history)

            // 4) Bugünkü haftalık görevler ve günlük görevleri resetle
            val cal = Calendar.getInstance()
            val todayDow = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY    -> "MONDAY"
                Calendar.TUESDAY   -> "TUESDAY"
                Calendar.WEDNESDAY -> "WEDNESDAY"
                Calendar.THURSDAY  -> "THURSDAY"
                Calendar.FRIDAY    -> "FRIDAY"
                Calendar.SATURDAY  -> "SATURDAY"
                Calendar.SUNDAY    -> "SUNDAY"
                else               -> ""
            }
            allTasks
                .filter { it.weekday.isNullOrBlank() || it.weekday == todayDow }
                .forEach {
                    if (it.isChecked) {
                        it.isChecked = false
                        taskDao.updateTask(it)
                    }
                }

            // 5) Bir sonraki reset alarmını yeniden planla
            resetTimeDao.getResetTime()?.let { rt ->
                scheduleNextReset(context, rt)
            }
        }
    }
    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleNextReset(context: Context, rt: ResetTime) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, ResetReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, rt.resetHour)
            set(Calendar.MINUTE, rt.resetMinute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime, pi)
        } catch (se: SecurityException) {
            // İzin yoksa inexact de olsa planlayalım
            am.set(AlarmManager.RTC_WAKEUP, nextTime, pi)
        }
    }
}