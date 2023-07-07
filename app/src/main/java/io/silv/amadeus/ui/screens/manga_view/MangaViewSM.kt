package io.silv.amadeus.ui.screens.manga_view

import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.usecase.CombineMangaChapterInfo
import io.silv.manga.local.workers.ChapterDeletionWorker
import io.silv.manga.local.workers.ChapterDeletionWorkerTag
import io.silv.manga.local.workers.ChapterDownloadWorker
import io.silv.manga.local.workers.ChapterDownloadWorkerTag
import io.silv.manga.sync.anyRunning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MangaViewSM(
    combineMangaChapterInfo: CombineMangaChapterInfo,
    private val workManager: WorkManager,
    initialManga: DomainManga
): AmadeusScreenModel<MangaViewEvent>() {

    private val loading = combineMangaChapterInfo.loading
        .stateInUi(false)

    private val downloadingIds = MutableStateFlow<List<String>>(emptyList())

    val downloadingOrDeleting = combine(
        workManager.getWorkInfosByTagFlow(ChapterDownloadWorkerTag)
            .map { it.anyRunning() },
        workManager.getWorkInfosByTagFlow(ChapterDeletionWorkerTag)
            .map { it.anyRunning() },
        downloadingIds
    ) { downloading, deleting, ids ->
        if (downloading || deleting) {
            ids
        } else {
            emptyList()
        }
    }
        .stateInUi(emptyList())

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

    fun deleteChapterImages(chapterIds: List<String>) = coroutineScope.launch {
        downloadingIds.emit(chapterIds)
        workManager.enqueue(
            ChapterDeletionWorker
                .deletionWorkRequest(chapterIds)
        )
    }

    fun downloadChapterImages(chapterIds: List<String>) = coroutineScope.launch {
        downloadingIds.emit(chapterIds)
        workManager.enqueue(
            ChapterDownloadWorker.downloadWorkRequest(
                chapterIds,
                chapterInfoUiState.value.manga.id
            )
        )
    }
}


