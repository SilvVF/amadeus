package io.silv.database.entity.manga.remotekeys

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import io.silv.database.entity.list.SeasonalListEntity
import io.silv.database.entity.manga.SourceMangaResource

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = SourceMangaResource::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("manga_id"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SeasonalListEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("season_id"),
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
data class SeasonalRemoteKey(

    @ColumnInfo("manga_id", index = true)
    val mangaId: String,

    @ColumnInfo("season_id", index = true)
    val seasonId: String,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
)
