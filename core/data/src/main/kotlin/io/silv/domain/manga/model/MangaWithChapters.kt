package io.silv.domain.manga.model

import androidx.compose.runtime.Stable
import io.silv.domain.chapter.model.Chapter
import kotlinx.collections.immutable.ImmutableList

@Stable
data class MangaWithChapters(
    val manga: Manga,
    val chapters: ImmutableList<Chapter>,
)
