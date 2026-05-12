package com.example.todolist

import androidx.room.*

@Dao
interface TodolistDao {
    @Query("SELECT * FROM lists WHERE userId = :uid ORDER BY sortOrder ASC")
    suspend fun getAllLists(uid: String): List<Todolist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: Todolist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todolist: Todolist)

    @Delete
    suspend fun delete(todolist: Todolist)

    @Update
    suspend fun updateList(vararg todoLists: Todolist)
}