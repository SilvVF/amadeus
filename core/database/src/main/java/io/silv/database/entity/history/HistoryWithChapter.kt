package io.silv.database.entity.history

import androidx.room.Embedded
import androidx.room.Relation
import io.silv.database.entity.chapter.ChapterEntity


data class HistoryWithChapter(

    @Embedded
    val historyEntity: HistoryEntity,

    @Relation(
        entity = ChapterEntity::class,
        parentColumn = "chapter_id",
        entityColumn = "id",
    )
    val chapter: ChapterEntity
)
