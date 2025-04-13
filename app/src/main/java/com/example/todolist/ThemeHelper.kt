package com.example.todolist

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {
    const val PREF_NAME = "theme_pref"
    const val KEY_THEME = "app_theme"

    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    fun applyTheme(themePref: String) {
        when (themePref) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun saveTheme(context: Context, themePref: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, themePref).apply()
    }

    fun loadTheme(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, THEME_LIGHT) ?: THEME_LIGHT
    }
}
