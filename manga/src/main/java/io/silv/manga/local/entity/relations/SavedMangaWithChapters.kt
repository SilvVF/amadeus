package io.silv.manga.local.entity.relations

import androidx.room.Embedded
import androidx.room.Relation
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.SavedMangaEntity

data class SavedMangaWithChapters(

    @Embedded val manga: SavedMangaEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_id"
    )
    val chapters: List<ChapterEntity>
)

