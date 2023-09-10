package io.silv.manga.local.entity.relations

import androidx.room.Embedded
import androidx.room.Relation
import io.silv.manga.local.entity.MangaUpdateEntity
import io.silv.manga.local.entity.SavedMangaEntity


data class MangaUpdateEntityWithManga(

    @Embedded val update: MangaUpdateEntity,

    @Relation(
        parentColumn = "saved_manga_id",
        entityColumn = "id"
    )
    val manga: SavedMangaEntity
)
