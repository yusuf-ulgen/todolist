package com.example.todolist

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {
    private const val PREFS_NAME = "onboard_prefs"
    private const val KEY_FIRST_LAUNCH = "first_launch"

    private const val KEY_MIGRATION_DONE = "migration_done"
    private const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code"
    private const val KEY_LAST_SEEN_VERSION_NAME = "last_seen_version_name"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isFirstLaunch(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setLaunched(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun isMigrationDone(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MIGRATION_DONE, false)
    }

    fun setMigrationDone(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_MIGRATION_DONE, true).apply()
    }

    fun getLastSeenVersionCode(context: Context): Int {
        return getPrefs(context).getInt(KEY_LAST_SEEN_VERSION_CODE, 0)
    }

    fun setLastSeenVersionCode(context: Context, versionCode: Int) {
        getPrefs(context).edit().putInt(KEY_LAST_SEEN_VERSION_CODE, versionCode).apply()
    }

    fun getLastSeenVersionName(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_SEEN_VERSION_NAME, "") ?: ""
    }

    fun setLastSeenVersionName(context: Context, versionName: String) {
        getPrefs(context).edit().putString(KEY_LAST_SEEN_VERSION_NAME, versionName).apply()
    }
}