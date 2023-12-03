package io.silv.model

import androidx.compose.runtime.Stable
import io.silv.database.entity.chapter.ChapterEntity

@Stable
data class SavableMangaWithChapters(
    val savableManga: SavableManga?,
    val chapters: List<ChapterEntity>
)