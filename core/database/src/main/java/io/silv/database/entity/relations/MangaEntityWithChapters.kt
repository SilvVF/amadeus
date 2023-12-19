package io.silv.database.entity.relations

import androidx.room.Embedded
import androidx.room.Relation
import io.silv.database.entity.chapter.ChapterEntity
import io.silv.database.entity.manga.MangaEntity

data class MangaEntityWithChapters(

    @Embedded val manga: MangaEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_id",
    )
    val chapters: List<ChapterEntity>,
)
