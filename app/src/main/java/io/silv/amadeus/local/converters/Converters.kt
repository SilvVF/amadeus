package io.silv.amadeus.local.converters

import androidx.room.TypeConverter
import io.silv.amadeus.local.entity.TrackingState

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
    fun trackingStateToInt(trackingState: TrackingState): Int {
       return trackingState.ordinal
    }

    @TypeConverter
    fun intToTrackingState(int: Int): TrackingState {
        return TrackingState.values()[int]
    }
}