package io.silv.database.entity.relations

import androidx.room.Embedded
import androidx.room.Relation
import io.silv.database.entity.chapter.ChapterEntity
import io.silv.database.entity.manga.SavedMangaEntity

data class SavedMangaWithChapters(
    @Embedded val manga: SavedMangaEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "manga_id",
    )
    val chapters: List<ChapterEntity>,
)
