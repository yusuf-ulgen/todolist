package com.example.todolist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationPrefDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pref: NotificationPref)

    @Query("SELECT * FROM notification_pref WHERE userId = :uid LIMIT 1")
    suspend fun getPref(uid: String): NotificationPref?
}