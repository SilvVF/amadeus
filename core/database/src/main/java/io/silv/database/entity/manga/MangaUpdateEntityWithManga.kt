package io.silv.database.entity.manga

import androidx.room.Embedded
import androidx.room.Relation
import io.silv.database.entity.manga.MangaEntity
import io.silv.database.entity.manga.MangaUpdateEntity

data class MangaUpdateEntityWithManga(

    @Embedded val update: MangaUpdateEntity,

    @Relation(
        parentColumn = "saved_manga_id",
        entityColumn = "id",
    )
    val manga: MangaEntity,
)
