package io.silv.model

import io.silv.database.entity.chapter.ChapterEntity

data class SavableMangaWithChapters(
    val savableManga: SavableManga?,
    val chapters: List<ChapterEntity>
)