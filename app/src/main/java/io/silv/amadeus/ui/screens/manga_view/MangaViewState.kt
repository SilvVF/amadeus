package io.silv.amadeus.ui.screens.manga_view

import androidx.compose.runtime.Immutable
import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.domain.models.DomainManga

sealed class ChapterListState(
    open val chapters: List<DomainChapter> = emptyList(),
) {
    object Loading: ChapterListState()
    data class Success(override val chapters: List<DomainChapter>): ChapterListState(chapters)
    data class Failure(val message: String): ChapterListState()
}

sealed class CoverArtState(
    open val art: Map<String,String> = emptyMap()
) {
    object Loading: CoverArtState()
    data class Success(override val art: Map<String, String>): CoverArtState(art)
    data class Failure(val message: String): CoverArtState()
}

@Immutable
data class MangaViewState(
    val manga: DomainManga,
    val coverArtState: CoverArtState = CoverArtState.Loading,
    val chapterListState: ChapterListState = ChapterListState.Loading,
)
