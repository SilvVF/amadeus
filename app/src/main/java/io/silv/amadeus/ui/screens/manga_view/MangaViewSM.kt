package io.silv.amadeus.ui.screens.manga_view

import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.usecase.CombineMangaChapterInfo
import kotlinx.coroutines.flow.map

class MangaViewSM(
    combineMangaChapterInfo: CombineMangaChapterInfo,
    initialManga: DomainManga
): AmadeusScreenModel<MangaViewEvent>() {

    private val loading = combineMangaChapterInfo.loading
        .stateInUi(false)

    val chapterInfoUiState = combineMangaChapterInfo(initialManga.id)
        .map { state ->
            MangaViewState(
                coverArtState = state.volumeImages?.let { CoverArtState.Success(it) }
                    ?: if (loading.value)
                        CoverArtState.Loading
                    else
                        CoverArtState.Failure("Failed To Load"),
                chapterListState = state.chapterInfo?.let { ChapterListState.Success(it) }
                    ?: if (loading.value)
                        ChapterListState.Loading
                    else
                        ChapterListState.Failure("Failed To Load"),
                manga = state.domainManga
            )
        }
        .stateInUi(
            MangaViewState(
                manga = initialManga,
                coverArtState = CoverArtState.Loading,
                chapterListState = ChapterListState.Loading
            )
        )
}


