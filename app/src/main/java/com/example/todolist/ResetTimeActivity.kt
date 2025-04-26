package com.example.todolist

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        // 1) Var olan reset zamanını oku, UI’ya bas
        GlobalScope.launch(Dispatchers.IO) {
            val saved = resetTimeDao.getResetTime()
            withContext(Dispatchers.Main) {
                saved?.let {
                    showSavedTime(it.resetHour, it.resetMinute)
                    binding.timePicker.hour   = it.resetHour
                    binding.timePicker.minute = it.resetMinute
                }
            }
        }

        // 2) Kaydet butonuna tıklanınca:
        binding.saveResetTimeButton.setOnClickListener {
            val hour   = binding.timePicker.hour
            val minute = binding.timePicker.minute

            // 3) Veritabanına kaydet
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

                    // 4) AlarmManager ile günlük reset alarmını kur
                    scheduleDailyResetAlarm(hour, minute)
                }
            }
        }
    }

    // Kaydedilen saati ekranda gösterir
    private fun showSavedTime(hour: Int, minute: Int) {
        val text = String.format("%02d:%02d", hour, minute)
        binding.savedResetTimeValue.text = text
    }

    // AlarmManager’ı kuran yardımcı fonksiyon
    @Suppress("ScheduleExactAlarm")
    private fun scheduleDailyResetAlarm(resetHour: Int, resetMinute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent       = Intent(this, ResetReceiver::class.java)
        val pending      = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Bugünkü reset zamanını Calendar ile hesapla;
        // geçmişse bir gün ekle
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, resetHour)
            set(Calendar.MINUTE, resetMinute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Tam saatte tetikle, uyku modunda bile
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            pending
        )
    }
}