package com.example.todolist

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.todolist.databinding.ActivityThemeBinding

class ThemeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityThemeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(ThemeHelper.loadTheme(this))
        super.onCreate(savedInstanceState)

        binding = ActivityThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Tema Seçimi"
    }

    // Beyaz kısma tıklanırsa beyaz tema uygula ve MainActivity'e dön
    fun onWhiteClick() {
        ThemeHelper.saveTheme(this, ThemeHelper.THEME_LIGHT)
        ThemeHelper.applyTheme(ThemeHelper.THEME_LIGHT)
        val intent = Intent(this, ListelerimActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    // Siyah kısma tıklanırsa siyah tema uygula ve MainActivity'e dön
    fun onBlackClick() {
        ThemeHelper.saveTheme(this, ThemeHelper.THEME_DARK)
        ThemeHelper.applyTheme(ThemeHelper.THEME_DARK)
        val intent = Intent(this, ListelerimActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
}