package io.silv.data.manga.model

import androidx.compose.runtime.Stable
import io.silv.data.chapter.Chapter

@Stable
data class MangaWithChapters(
    val manga: Manga,
    val chapters: List<Chapter>,
)
