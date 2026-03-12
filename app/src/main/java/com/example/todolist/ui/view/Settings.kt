package com.example.todolist

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.todolist.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth

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

        // Admin Feedback Tıklama İşlemi
        binding.feedbackListButton.setOnClickListener {
            val seenUid  = FirebaseAuth.getInstance().currentUser?.uid
            val adminUid = "NvKPJHa85rfVgFId0r46FcKGq5u1"

            if (seenUid != adminUid) {
                // Sadece bu mesaj görünsün:
                Toast.makeText(this, "Yetkiniz yok.", Toast.LENGTH_SHORT).show()
            } else {
                // Admin ise ekrana geçiş:
                startActivity(Intent(this, AdminFeedbackActivity::class.java))
            }
        }
    }
}