package com.example.todolist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TodolistDao {
    @Query("SELECT * FROM lists ORDER BY id ASC")
    suspend fun getAllLists(): List<Todolist>

    @Insert
    suspend fun insertList(list: Todolist): Long
}

