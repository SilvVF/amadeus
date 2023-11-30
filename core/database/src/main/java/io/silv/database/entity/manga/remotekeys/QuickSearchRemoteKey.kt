package io.silv.database.entity.manga.remotekeys

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class QuickSearchRemoteKey(
    @ColumnInfo("manga_id")
    val mangaId: String,
    val offset: Int,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)