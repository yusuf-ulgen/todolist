package com.example.todolist

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.todolist.databinding.ActivityResetTimeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResetTimeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResetTimeBinding
    private lateinit var db: AppDatabase
    private lateinit var resetTimeDao: ResetTimeDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResetTimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Room database setup
        db = AppDatabase.getDatabase(applicationContext)
        resetTimeDao = db.resetTimeDao()

        // Kaydet butonuna tıklanırsa, saati veritabanına kaydet
        binding.saveResetTimeButton.setOnClickListener {
            val selectedHour = binding.timePicker.hour
            val selectedMinute = binding.timePicker.minute

            val resetTime = ResetTime(resetHour = selectedHour, resetMinute = selectedMinute)

            // Veritabanına kaydet
            GlobalScope.launch(Dispatchers.IO) {
                resetTimeDao.insertResetTime(resetTime)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ResetTimeActivity, "Reset Saati Kaydedildi", Toast.LENGTH_SHORT).show()
                    finish() // Activity'yi kapat
                }
            }
        }
    }
}
