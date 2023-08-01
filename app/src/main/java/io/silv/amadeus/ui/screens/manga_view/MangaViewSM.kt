package io.silv.amadeus.ui.screens.manga_view

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.repositorys.base.ProtectedResources
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.usecase.CombineMangaChapterInfo
import io.silv.manga.local.workers.ChapterDeletionWorker
import io.silv.manga.local.workers.ChapterDeletionWorkerTag
import io.silv.manga.local.workers.ChapterDownloadWorker
import io.silv.manga.local.workers.ChapterDownloadWorkerTag
import io.silv.manga.sync.anyRunning
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MangaViewSM(
    combineMangaChapterInfo: CombineMangaChapterInfo,
    private val savedMangaRepository: SavedMangaRepository,
    private val workManager: WorkManager,
    private val initialManga: DomainManga
): AmadeusScreenModel<MangaViewEvent>() {

    init {
        ProtectedResources.ids.add(initialManga.id)
    }

    private val loading = combineMangaChapterInfo.loading(initialManga.id)
        .stateInUi(false)


    val downloadingOrDeleting = combine(
        workManager.getWorkInfosByTagFlow(ChapterDownloadWorkerTag)
            .map { it.anyRunning() },
        workManager.getWorkInfosByTagFlow(ChapterDeletionWorkerTag)
            .map { it.anyRunning() },
        ChapterDownloadWorker.downloadingIds
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
                manga = state.domainManga ?: initialManga
            )
        }
        .stateInUi(
            MangaViewState(
                manga = initialManga,
                coverArtState = CoverArtState.Loading,
                chapterListState = ChapterListState.Loading
            )
        )

    fun bookmarkManga(id: String) = coroutineScope.launch {
        savedMangaRepository.bookmarkManga(id)
    }

    fun deleteChapterImages(chapterIds: List<String>) = coroutineScope.launch {
        workManager.enqueue(
            ChapterDeletionWorker
                .deletionWorkRequest(chapterIds)
        )
    }

    fun downloadChapterImages(chapterIds: List<String>) = coroutineScope.launch {
        workManager.enqueueUniqueWork(
            chapterIds.toString(),
            ExistingWorkPolicy.KEEP,
            ChapterDownloadWorker.downloadWorkRequest(
                chapterIds,
                chapterInfoUiState.value.manga.id
            )
        )
    }

    override fun onDispose() {
        super.onDispose()
        ProtectedResources.ids.remove(initialManga.id)
    }
}


