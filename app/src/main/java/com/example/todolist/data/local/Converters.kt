package com.example.todolist.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromAnyToString(value: Any?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun fromStringToAny(value: String?): Any? {
        return value
    }
}
