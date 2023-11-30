package io.silv.database.entity.manga.remotekeys

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PopularRemoteKey(
    @ColumnInfo("manga_id")
    val mangaId: String,
    val offset: Int = 0,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
)