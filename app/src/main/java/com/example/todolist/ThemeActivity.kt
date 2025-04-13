package com.example.todolist

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.todolist.databinding.ActivityThemeBinding

class ThemeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityThemeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(ThemeHelper.loadTheme(this)) // ⬅ önce bu
        super.onCreate(savedInstanceState)

        binding = ActivityThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Tema Seçimi"

        binding.lightThemeButton.setOnClickListener {
            ThemeHelper.saveTheme(this, ThemeHelper.THEME_LIGHT)
            ThemeHelper.applyTheme(ThemeHelper.THEME_LIGHT)
            Toast.makeText(this, "Açık tema seçildi", Toast.LENGTH_SHORT).show()
            recreate()
        }

        binding.darkThemeButton.setOnClickListener {
            ThemeHelper.saveTheme(this, ThemeHelper.THEME_DARK)
            ThemeHelper.applyTheme(ThemeHelper.THEME_DARK)
            Toast.makeText(this, "Koyu tema seçildi", Toast.LENGTH_SHORT).show()
            recreate()
        }

        binding.customThemeButton.setOnClickListener {
            Toast.makeText(this, "Özel tema yakında eklenecek", Toast.LENGTH_SHORT).show()
        }
    }
}
