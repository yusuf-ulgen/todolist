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

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 1) Görev bilgileri
        val taskId      = intent.getIntExtra("taskId", 0)
        val taskContent = intent.getStringExtra("taskContent") ?: "Görev zamanı!"

        // 2) Kanal oluştur (O ve üzeri için)
        val channelId = "task_channel"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId,
                "Görev Hatırlatmaları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Haftalık & Günlük görev bildirimleri"
                enableLights(true)
                lightColor = ContextCompat.getColor(context, R.color.primary)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 200, 200)
            }
            nm.createNotificationChannel(chan)
        }

        // 3) Uygulamayı açacak PendingIntent
        val openIntent = Intent(context, ListelerimActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(
            context, taskId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4) Bildirimi inşa et
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.icon)                   // kendi ikonunuz
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setContentTitle("Görev Zamanı")
            .setContentText(taskContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(taskContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        val notifManager = NotificationManagerCompat.from(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PermissionChecker.PERMISSION_GRANTED) {
            notifManager.notify(taskId, notif)
        }

        // 5) Gönder
        NotificationManagerCompat.from(context)
            .notify(taskId, notif)
    }
}