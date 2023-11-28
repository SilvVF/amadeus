package io.silv.database.entity.relations

import androidx.room.Embedded
import androidx.room.Relation
import io.silv.database.entity.manga.MangaUpdateEntity
import io.silv.database.entity.manga.SavedMangaEntity


data class MangaUpdateEntityWithManga(

    @Embedded val update: MangaUpdateEntity,

    @Relation(
        parentColumn = "saved_manga_id",
        entityColumn = "id"
    )
    val manga: SavedMangaEntity
)
