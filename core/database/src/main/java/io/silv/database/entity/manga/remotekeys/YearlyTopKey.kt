package io.silv.database.entity.manga.remotekeys

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import io.silv.database.entity.manga.SourceMangaResource

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = SourceMangaResource::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("manga_id"),
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
data class YearlyTopKey(

    @ColumnInfo("manga_id", index = true)
    val mangaId: String,

    val tagIds: List<String>,

    val tagIdToPlacement: Map<String, Int>,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
)