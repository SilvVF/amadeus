package io.silv.database.entity.manga.remotekeys

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import io.silv.database.entity.manga.SourceMangaResource


@Entity(
    foreignKeys = [ForeignKey(
        entity = SourceMangaResource::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("manga_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class RemoteKey(

    @ColumnInfo("manga_id", index = true)
    val mangaId: String,

    val offset: Int,

    @ColumnInfo(name = "query_id", index = true)
    val queryId: String,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
)



