package com.example.todolist

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.todolist.databinding.ActivitySettingsBinding

class Settings : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tema Seçimi Butonuna Tıklama İşlemi
        binding.themeButton.setOnClickListener {
            val intent = Intent(this, ThemeActivity::class.java)
            startActivity(intent)
        }

        // Resetleme Zamanı Butonuna Tıklama İşlemi
        binding.resetTimeButton.setOnClickListener {
            val intent = Intent(this, ResetTimeActivity::class.java)
            startActivity(intent)
        }

        // İstatistiklerim Butonuna Tıklama İşlemi
        binding.statisticsButton.setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            startActivity(intent)
        }

        // Şifre Butonuna Tıklama İşlemi
        binding.changePasswordButton.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }
    }
}
