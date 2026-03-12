package com.example.todolist

class NotificationPreferenceRepository(private val dao: NotificationPrefDao) {

    suspend fun saveKind(kind: Int) {
        dao.upsert(NotificationPref(kind = kind))
    }

    suspend fun loadKind(): Int {
        // eğer hiç pref yoksa -1 dönecek
        return dao.getPref()?.kind ?: -1
    }
}