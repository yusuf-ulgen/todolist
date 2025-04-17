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

        binding.themeButton.setOnClickListener {
            val intent = Intent(this, ThemeActivity::class.java)
            startActivity(intent)
        }
        // Resetleme saati butonuna tıklanırsa, ResetTimeActivity'ye geçiş yapılır
        binding.resetTimeButton.setOnClickListener {
            val intent2 = Intent(this, ResetTimeActivity::class.java)
            startActivity(intent2)
        }
    }
}