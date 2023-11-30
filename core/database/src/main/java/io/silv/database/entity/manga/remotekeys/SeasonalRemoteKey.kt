package io.silv.database.entity.manga.remotekeys

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import io.silv.database.entity.manga.SourceMangaResource

@Entity(
    foreignKeys = [ForeignKey(
        entity = SourceMangaResource::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("manga_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class SeasonalRemoteKey(
    @ColumnInfo("manga_id", index = true)
    val mangaId: String,
    @ColumnInfo("season_id")
    val seasonId: String,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
)


data class SeasonalRemoteKeyWithSourceManga(

    @Embedded
    val manga: SourceMangaResource,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_id"
    )
    val key: SeasonalRemoteKey,
)