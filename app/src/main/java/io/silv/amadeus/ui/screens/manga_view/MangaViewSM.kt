package io.silv.amadeus.ui.screens.manga_view

import androidx.compose.runtime.Immutable
import androidx.work.Data
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.repositorys.CombinedMangaChapterInfoVolumeImagesRepository
import io.silv.manga.sync.SyncManager
import io.silv.manga.sync.SyncManager.Companion.MANGA_ID_KEY
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class MangaViewSM(
    private val syncManager: SyncManager,
    combinedSavedMangaChapterRepository: CombinedMangaChapterInfoVolumeImagesRepository,
    initialManga: DomainManga
): AmadeusScreenModel<MangaViewEvent>() {

    private val loading = syncManager.isSyncing
        .stateInUi(false)

    init {
        loadInfo(initialManga.id)
    }

    val chapterInfoUiState = combinedSavedMangaChapterRepository
        .observeManga(initialManga.id)
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
        .onEach { println(it.coverArtState) }
        .stateInUi(
            MangaViewState(
                manga = initialManga,
                coverArtState = CoverArtState.Loading,
                chapterListState = ChapterListState.Loading
            )
        )

    fun loadInfo(mangaId: String) {
        syncManager.requestSync(
            Data.Builder()
                .putString(MANGA_ID_KEY, mangaId)
                .build()
        )
    }
}


