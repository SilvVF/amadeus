package io.silv.database

import androidx.room.TypeConverter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.TreeMap

class Converters {

    @TypeConverter
    fun fromStrList(list: List<String>): String {
        return list.joinToString(separator ="<divider>")
    }

    @TypeConverter
    fun strToStrList(string: String): List<String> {
        return string
            .ifEmpty { return emptyList() }
            .split("<divider>")
            .ifEmpty { return emptyList() }
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
    fun fromStringIntMap(map: Map<String, Int>): String {
        val sortedMap = TreeMap(map)
        return sortedMap.keys.joinToString(separator = ",").plus("<divider>")
            .plus(sortedMap.values.joinToString(separator = ","))
    }

    @TypeConverter
    fun toStringIntMap(value: String): Map<String, Int> {
        return value.ifEmpty { return emptyMap() }.split("<divider>").run {

            val keys = getOrNull(0)?.split(",")
            val values = getOrNull(1)?.split(",")?.map { it.toIntOrNull() ?: 0 }

            val res = hashMapOf<String, Int>()
            keys?.forEachIndexed { index, s ->
                res[s] = values?.getOrNull(index) ?: 0
            }
            res
        }
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
    fun fromLongToLocalDateTime(epochSeconds: Long): LocalDateTime {
        return Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(TimeZone.currentSystemDefault())
    }

    @TypeConverter
    fun fromLocalDateTimeToLong(localDateTime: LocalDateTime): Long {
        return localDateTime.toInstant(TimeZone.currentSystemDefault()).epochSeconds
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