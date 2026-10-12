package com.roadsensevision.data.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun stringListToJson(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun jsonToStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(",")
}
