package com.example.todolist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 1) taskId’yi Int olarak alın
        val taskId = intent.getIntExtra("taskId", 0)
        val content = intent.getStringExtra("taskContent") ?: "Görev zamanı!"

        // 2) Bildirim kanalı (Android O+ için)
        val channelId = "task_channel"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId,
                "Görev Hatırlatmaları",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(chan)
        }

        // 3) Bildirim inşa et (MUST have smallIcon)
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.pin_night)  // ← Öylesine görsel
            .setContentTitle("Görev Zamanı")
            .setContentText(content)
            .setAutoCancel(true)
            .build()

        // 4) Notify
        nm.notify(taskId, notif)
    }
}