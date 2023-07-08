package io.silv.manga.local.entity.converters

import androidx.room.TypeConverter
import java.util.TreeMap

class Converters {

    @TypeConverter
    fun fromStrList(list: List<String>): String {
        return list.joinToString(separator ="<divider>")
    }

    @TypeConverter
    fun strToStrList(string: String): List<String> {
        return string.ifEmpty { return emptyList() }.split("<divider>")
    }

    @TypeConverter
    fun fromIntList(list: List<Int>): String {
        return list.joinToString(separator ="<divider>")
    }

    @TypeConverter
    fun strToIntList(string: String): List<Int> {
        return string.ifEmpty { return emptyList() }
            .split("<divider>")
            .map { it.toInt() }
    }

    @TypeConverter
    fun fromIntMap(map: Map<Int, String>): String {
        val sortedMap = TreeMap(map)
        return sortedMap.keys.joinToString(separator = ",").plus("<divider>")
            .plus(sortedMap.values.joinToString(separator = ","))
    }

    @TypeConverter
    fun toIntMap(value: String): Map<Int, String> {
        return value.ifEmpty { return emptyMap() }.split("<divider>").run {

            val keys = getOrNull(0)?.split(",")?.map { it.toIntOrNull() ?: 0 }
            val values = getOrNull(1)?.split(",")?.map { it }

            val res = hashMapOf<Int, String>()
            keys?.forEachIndexed { index, s ->
                res[s] = values?.getOrNull(index) ?: ""
            }
            res
        }
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