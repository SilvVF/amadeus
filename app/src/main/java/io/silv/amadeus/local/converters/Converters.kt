package io.silv.amadeus.local.converters

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromStrList(list: List<String>): String {
        return list.toString().removePrefix("[").removeSuffix("]")
    }

    @TypeConverter
    fun strToStrList(string: String): List<String> {
        return string.split(",")
    }
}