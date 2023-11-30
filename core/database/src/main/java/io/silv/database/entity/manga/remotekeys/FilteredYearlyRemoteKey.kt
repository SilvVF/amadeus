package io.silv.database.entity.manga.remotekeys

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class FilteredYearlyRemoteKey(

    @ColumnInfo("manga_id")
    val mangaId: String,
    val topTags: List<String>,
    val topTagPlacement: Map<String, Int>,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
)