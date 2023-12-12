package io.silv.model

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList

@Stable
data class SavableMangaWithChapters(
    val savableManga: SavableManga,
    val chapters: ImmutableList<SavableChapter>
)