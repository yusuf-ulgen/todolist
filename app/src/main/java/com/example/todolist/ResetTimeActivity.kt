package com.example.todolist

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.todolist.databinding.ActivityResetTimeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ResetTimeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResetTimeBinding
    private lateinit var db: AppDatabase
    private lateinit var resetTimeDao: ResetTimeDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetTimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(applicationContext)
        resetTimeDao = db.resetTimeDao()

        binding.timePicker.setIs24HourView(true)

        val color = ContextCompat.getColor(this, R.color.creamOnBackground)

        // Sistem ID’lerinden “hour” ve “minute” alanlarını bulup renk ata
        listOf("hour", "minute").forEach { name ->
            val id = Resources.getSystem().getIdentifier(name, "id", "android")
            binding.timePicker.findViewById<TextView>(id)?.setTextColor(color)
        }

        GlobalScope.launch(Dispatchers.IO) {
            val saved = resetTimeDao.getResetTime()
            withContext(Dispatchers.Main) {
                saved?.let {
                    showSavedTime(it.resetHour, it.resetMinute)
                    binding.timePicker.hour = it.resetHour
                    binding.timePicker.minute = it.resetMinute
                }
            }
        }

        binding.saveResetTimeButton.setOnClickListener {
            val hour = binding.timePicker.hour
            val minute = binding.timePicker.minute

            val resetTime = ResetTime(resetHour = hour, resetMinute = minute)
            GlobalScope.launch(Dispatchers.IO) {
                resetTimeDao.deleteAll()
                resetTimeDao.insertResetTime(resetTime)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ResetTimeActivity,
                        "Reset Saati Kaydedildi",
                        Toast.LENGTH_SHORT
                    ).show()
                    showSavedTime(hour, minute)

                    scheduleDailyResetAlarm(hour, minute)
                }
            }
        }
    }

    private fun showSavedTime(hour: Int, minute: Int) {
        val text = String.format("%02d:%02d", hour, minute)
        binding.savedResetTimeValue.text = text
    }

    private fun scheduleDailyResetAlarm(resetHour: Int, resetMinute: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    this,
                    "Lütfen uygulamaya alarm kurma izni verin.",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                return
            }
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ResetReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, resetHour)
            set(Calendar.MINUTE, resetMinute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            pending
        )
    }
}