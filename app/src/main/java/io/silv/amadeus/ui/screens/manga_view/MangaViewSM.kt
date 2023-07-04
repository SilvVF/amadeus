package io.silv.amadeus.ui.screens.manga_view

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.work.Data
import androidx.work.WorkInfo
import cafe.adriel.voyager.core.model.ScreenModel
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.domain.models.DomainCoverArt
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.repositorys.CombinedMangaChapterInfoVolumeImagesRepository
import io.silv.manga.sync.ChapterInfoSyncWorker
import io.silv.manga.sync.SyncManager
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class MangaViewSM(
    private val syncManager: SyncManager,
    private val combinedSavedMangaChapterRepository: CombinedMangaChapterInfoVolumeImagesRepository,
    private val initialManga: DomainManga
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
                    ?: if (loading.value) CoverArtState.Loading else CoverArtState.Failure("Failed To Load"),
                chapterListState = state.chapterInfo?.let { ChapterListState.Success(it) }
                    ?: if (loading.value) ChapterListState.Loading else ChapterListState.Failure("Failed To Load"),
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
                .putString(ChapterInfoSyncWorker.MANGA_ID_KEY, mangaId)
                .build()
        )
    }
}

sealed interface MangaViewEvent

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
