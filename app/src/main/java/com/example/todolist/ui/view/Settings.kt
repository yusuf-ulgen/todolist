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
        com.example.todolist.WindowInsetsHelper.applyTopBottomInsets(binding.root)

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

        // Web Sitemiz Butonuna Tıklama İşlemi
        binding.websiteButton.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://todolist.yusufulgen.com"))
                startActivity(intent)
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Şifre Butonuna Tıklama İşlemi
        binding.changePasswordButton.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        // Admin Feedback Tıklama İşlemi
        binding.feedbackListButton.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            val seenUid = user?.uid
            val seenEmail = user?.email
            val adminUid = "NvKPJHa85rfVgFId0r46FcKGq5u1"
            val testAdminEmail = "testadmin@gmail.com"

            if (seenUid == adminUid || seenEmail == testAdminEmail) {
                // Admin veya Test Hesabı ise ekrana geçiş:
                startActivity(Intent(this, AdminFeedbackActivity::class.java))
            } else {
                // Yetki yoksa:
                Toast.makeText(this, "Yetkiniz yok.", Toast.LENGTH_SHORT).show()
            }
        }

        // Yapımcının diğer içerikleri butonu
        binding.developerAppsButton.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/developer?id=Yusuf+Ulgen"))
                startActivity(intent)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}