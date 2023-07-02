package io.silv.amadeus.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MangaEntity(
    @PrimaryKey val mid: String,
    @ColumnInfo("tracking_state") val trackingState: TrackingState = TrackingState.NotTracked,
    @ColumnInfo("progress_state") val progressState: ProgressState = ProgressState.NotStarted,
    @ColumnInfo val volumes: List<String>,
)
