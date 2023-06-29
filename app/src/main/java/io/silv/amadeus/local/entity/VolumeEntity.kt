package io.silv.amadeus.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class VolumeEntity(
    @PrimaryKey val vid: String,
    @ColumnInfo("manga_id") val mangaId: String,
    @ColumnInfo("chapter_ids") val chapterIds: List<String>
)
