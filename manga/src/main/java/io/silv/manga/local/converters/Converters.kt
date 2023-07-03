package io.silv.manga.local.converters

import androidx.room.TypeConverter
import java.util.TreeMap

class Converters {

    @TypeConverter
    fun fromStrList(list: List<String>): String {
        return list.joinToString()
    }

    @TypeConverter
    fun strToStrList(string: String): List<String> {
        return string.split(",")
    }

    @TypeConverter
    fun fromStringMap(map: Map<String, String>): String {
        val sortedMap = TreeMap(map)
        return sortedMap.keys.joinToString(separator = ",").plus("<divider>")
            .plus(sortedMap.values.joinToString(separator = ","))
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        return value.split("<divider>").run {
            val keys = getOrNull(0)?.split(",")?.map { it }
            val values = getOrNull(1)?.split(",")?.map { it }

            val res = hashMapOf<String, String>()
            keys?.forEachIndexed { index, s ->
                res[s] = values?.getOrNull(index) ?: ""
            }
            res
        }
    }
}