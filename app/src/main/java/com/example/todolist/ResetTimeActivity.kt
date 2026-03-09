package com.example.todolist

import android.annotation.SuppressLint
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.todolist.databinding.ActivityResetTimeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ResetTimeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResetTimeBinding
    private lateinit var viewModel: TaskViewModel

    @SuppressLint("DiscouragedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetTimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = TaskRepository(
            db.taskDao(),
            db.todolistDao(),
            db.dailyStatDao(),
            db.taskHistoryDao(),
            db.notificationPrefDao(),
            db.resetTimeDao()
        )
        val factory = TaskViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]

        // 1) TimePicker'ı 24 saat formatına al ve rengini ayarla
        binding.timePicker.setIs24HourView(true)
        val color = ContextCompat.getColor(this, R.color.creamOnBackground)
        listOf("hour", "minute").forEach { name ->
            val id = Resources.getSystem().getIdentifier(name, "id", "android")
            binding.timePicker.findViewById<TextView>(id)
                ?.setTextColor(color)
        }

        // Observe reset time
        viewModel.resetTime.observe(this) { saved ->
            saved?.let {
                showSavedTime(it.resetHour, it.resetMinute)
                binding.timePicker.hour = it.resetHour
                binding.timePicker.minute = it.resetMinute
                binding.weekDaySpinner.setSelection(it.resetDay)
            }
        }
        viewModel.loadResetTime()

        binding.saveResetTimeButton.setOnClickListener {
            val hour   = binding.timePicker.hour
            val minute = binding.timePicker.minute
            val dayPos = binding.weekDaySpinner.selectedItemPosition

            // 1) Room’a upsert et
            viewModel.saveResetTime(
                ResetTime(
                    id = 0,
                    resetHour = hour,
                    resetMinute = minute,
                    resetDay = dayPos
                )
            )

            // 2) Haftalık alarmı planla
            scheduleWeeklyResetAlarm(hour, minute, dayPos)

            // 3) Geri bildirim
            Toast.makeText(
                this,
                "Reset saati kaydedildi: %02d:%02d, ${binding.weekDaySpinner.selectedItem}"
                    .format(hour, minute),
                Toast.LENGTH_SHORT
            ).show()
            showSavedTime(hour, minute)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showSavedTime(hour: Int, minute: Int) {
        binding.savedResetTimeValue.text = "%02d:%02d".format(hour, minute)
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleWeeklyResetAlarm(resetHour: Int, resetMinute: Int, resetDay: Int) {
        // Android 12+ izin kontrolü
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

        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ResetReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Spinner’daki index’e göre Calendar gününü al
        val dow = when (resetDay) {
            0 -> Calendar.MONDAY
            1 -> Calendar.TUESDAY
            2 -> Calendar.WEDNESDAY
            3 -> Calendar.THURSDAY
            4 -> Calendar.FRIDAY
            5 -> Calendar.SATURDAY
            6 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }

        // Şimdiki zaman ve hedef zaman
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dow)
            set(Calendar.HOUR_OF_DAY, resetHour)
            set(Calendar.MINUTE, resetMinute)
            set(Calendar.SECOND, 0)
            // Eğer hedef geçtiyse bir hafta ileri al
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        // Haftalık alarm
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            pi
        )
    }
}