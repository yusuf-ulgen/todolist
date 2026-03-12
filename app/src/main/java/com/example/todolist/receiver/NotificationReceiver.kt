package com.example.todolist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.Manifest
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val taskId      = intent.getIntExtra("taskId", 0)
                val taskContent = intent.getStringExtra("taskContent") ?: "Görev zamanı!"
                val listId      = intent.getLongExtra("listId", 1L)
                val isPinned    = intent.getBooleanExtra("isPinned", false)

                // 1) Bildirim Tercihini Oku
                val db = AppDatabase.getDatabase(context)
                val pref = db.notificationPrefDao().getPref()
                val kind = pref?.kind ?: 0 // Default: Herkes (0)

                // 2) Filtrele
                val shouldShow = when (kind) {
                    0 -> true                 // Her görev
                    1 -> isPinned            // Yalnız pinliler
                    else -> false             // Hiçbiri
                }

                if (!shouldShow) return@launch

                val channelId = "task_channel"
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val chan = NotificationChannel(
                        channelId,
                        "Görev Hatırlatmaları",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Haftalık & Günlük görev bildirimleri"
                    }
                    nm.createNotificationChannel(chan)
                }

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("listId", listId)
                }
                
                val pi = PendingIntent.getActivity(
                    context,
                    taskId,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notif = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.icon)
                    .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                    .setContentTitle("Görev Zamanı")
                    .setContentText(taskContent)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(taskContent))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build()

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PermissionChecker.PERMISSION_GRANTED) {
                    NotificationManagerCompat.from(context).notify(taskId, notif)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}