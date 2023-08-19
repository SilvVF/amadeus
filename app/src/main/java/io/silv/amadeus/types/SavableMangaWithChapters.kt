package io.silv.amadeus.types

import io.silv.manga.local.entity.ChapterEntity


data class SavableMangaWithChapters(
    val savableManga: SavableManga?,
    val chapters: List<ChapterEntity>
)