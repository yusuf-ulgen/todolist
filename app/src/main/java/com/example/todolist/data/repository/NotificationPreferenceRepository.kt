package com.example.todolist

class NotificationPreferenceRepository(private val dao: NotificationPrefDao) {

    suspend fun saveKind(kind: Int, userId: String) {
        dao.upsert(NotificationPref(kind = kind, userId = userId))
    }

    suspend fun loadKind(userId: String): Int {
        // eğer hiç pref yoksa -1 dönecek
        return dao.getPref(userId)?.kind ?: -1
    }
}