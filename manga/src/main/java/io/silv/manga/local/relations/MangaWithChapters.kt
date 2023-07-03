package io.silv.manga.local.relations

import androidx.room.Embedded
import androidx.room.Relation
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.MangaEntity

data class MangaWithChapters(

    @Embedded val manga: MangaEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_id"
    )
    val chapters: List<ChapterEntity>
)

