package io.silv.amadeus.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ChapterEntity(
    @PrimaryKey val cid: String,
    @ColumnInfo("volume_id") val volumeId: String,
    @ColumnInfo("uris") val uris: List<String>,
    @ColumnInfo("permanent") val permanent: Boolean
)
