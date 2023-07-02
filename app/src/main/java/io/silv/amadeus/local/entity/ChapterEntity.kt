package io.silv.amadeus.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock

@Entity
data class ChapterEntity(
    @PrimaryKey val cid: String,
    @ColumnInfo("progress_state") val progressState: ProgressState = ProgressState.NotStarted,
    @ColumnInfo("volume_id") val volumeId: String,
    @ColumnInfo("uris") val uris: List<String>,
    @ColumnInfo("created_at")val createdAtEpochSeconds: Long = Clock.System.now().epochSeconds,
    @ColumnInfo("permanent") val permanent: Boolean
)
