package com.example.todolist

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
    private val resetTimeDao by lazy {
        AppDatabase.getDatabase(applicationContext).resetTimeDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetTimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) TimePicker'ı 24 saat formatına al ve rengini ayarla
        binding.timePicker.setIs24HourView(true)
        val color = ContextCompat.getColor(this, R.color.creamOnBackground)
        listOf("hour", "minute").forEach { name ->
            val id = Resources.getSystem().getIdentifier(name, "id", "android")
            binding.timePicker.findViewById<TextView>(id)
                ?.setTextColor(color)
        }

        // 2) Daha önce kaydedilen saati oku ve göster
        GlobalScope.launch(Dispatchers.IO) {
            resetTimeDao.getResetTime()?.let { saved ->
                withContext(Dispatchers.Main) {
                    showSavedTime(saved.resetHour, saved.resetMinute)
                    binding.timePicker.hour   = saved.resetHour
                    binding.timePicker.minute = saved.resetMinute
                }
            }
        }

        // 3) Kaydet butonu
        binding.saveResetTimeButton.setOnClickListener {
            val hour   = binding.timePicker.hour
            val minute = binding.timePicker.minute

            // a) Room'a upsert et
            GlobalScope.launch(Dispatchers.IO) {
                resetTimeDao.upsert( ResetTime(resetHour = hour, resetMinute = minute) )
            }

            // b) Alarm'ı (yeniden) planla
            scheduleDailyResetAlarm(hour, minute)

            // c) Kullanıcıya bilgi ver
            Toast.makeText(
                this,
                "Reset saati kaydedildi: %02d:%02d".format(hour, minute),
                Toast.LENGTH_SHORT
            ).show()
            showSavedTime(hour, minute)
        }
    }

    private fun showSavedTime(hour: Int, minute: Int) {
        binding.savedResetTimeValue.text = "%02d:%02d".format(hour, minute)
    }

    private fun scheduleDailyResetAlarm(resetHour: Int, resetMinute: Int) {
        // Android 12+ exact alarm izni
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                Toast.makeText(
                    this,
                    "Uygulamaya alarm kurma izni vermelisiniz.",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                )
                return
            }
        }

        // AlarmManager ve PendingIntent oluştur
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ResetReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Hedef zamanı hesapla
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, resetHour)
            set(Calendar.MINUTE, resetMinute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Alarmı eksiksiz planla
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            pi
        )
    }
}