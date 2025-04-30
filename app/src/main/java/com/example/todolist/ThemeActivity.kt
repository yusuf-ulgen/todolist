package com.example.todolist

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.todolist.databinding.ActivityThemeBinding

class ThemeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityThemeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Uygulanan temayı ilk başta uygula
        ThemeHelper.applyTheme(ThemeHelper.loadTheme(this))
        super.onCreate(savedInstanceState)

        binding = ActivityThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Tema Seçimi"
    }

    // Beyaz kısma tıklanırsa beyaz tema uygula ve MainActivity'e dön
    fun onWhiteClick(view: android.view.View) {
        ThemeHelper.saveTheme(this, ThemeHelper.THEME_LIGHT)
        ThemeHelper.applyTheme(ThemeHelper.THEME_LIGHT)
        Toast.makeText(this, "Açık tema seçildi", Toast.LENGTH_SHORT).show()
        // MainActivity'e dön
        val intent = Intent(this, MainActivity::class.java).apply {
            // Eğer zaten açıksa en üstteki Activity olarak getir
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    // Siyah kısma tıklanırsa siyah tema uygula ve MainActivity'e dön
    fun onBlackClick(view: android.view.View) {
        ThemeHelper.saveTheme(this, ThemeHelper.THEME_DARK)
        ThemeHelper.applyTheme(ThemeHelper.THEME_DARK)
        Toast.makeText(this, "Koyu tema seçildi", Toast.LENGTH_SHORT).show()
        // MainActivity'e dön
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
}