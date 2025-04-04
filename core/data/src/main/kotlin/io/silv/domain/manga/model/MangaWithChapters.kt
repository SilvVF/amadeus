package io.silv.domain.manga.model

import androidx.compose.runtime.Stable
import io.silv.domain.chapter.model.Chapter

@Stable
data class MangaWithChapters(
    val manga: Manga,
    val chapters: List<Chapter>,
)
