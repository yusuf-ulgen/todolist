package com.example.todolist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TodolistDao {
    @Query("SELECT * FROM lists ORDER BY sortOrder ASC")
    suspend fun getAllLists(): List<Todolist>

    @Insert
    suspend fun insertList(list: Todolist): Long

    @Insert
    suspend fun insert(todolist: Todolist)

    @Delete
    suspend fun delete(todolist: Todolist)

    @Update
    suspend fun updateList(vararg todoLists: Todolist)
}

