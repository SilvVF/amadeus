package io.silv.amadeus.ui.screens.manga_view

import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.ChapterToChapterEntityMapper
import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.base.ProtectedResources
import io.silv.manga.domain.repositorys.chapter.ChapterListRepository
import io.silv.manga.domain.repositorys.chapter.Resource
import io.silv.manga.domain.usecase.CombineMangaChapterInfo
import io.silv.manga.local.workers.ChapterDeletionWorker
import io.silv.manga.local.workers.ChapterDeletionWorkerTag
import io.silv.manga.local.workers.ChapterDownloadWorker
import io.silv.manga.local.workers.ChapterDownloadWorkerTag
import io.silv.manga.sync.anyRunning
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MangaViewSM(
    chapterInfoRepository: ChapterListRepository,
    combineMangaChapterInfo: CombineMangaChapterInfo,
    private val savedMangaRepository: SavedMangaRepository,
    private val workManager: WorkManager,
    private val initialManga: DomainManga
): AmadeusScreenModel<MangaViewEvent>() {

    init {
        ProtectedResources.ids.add(initialManga.id)
    }

    private val loading = false

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

    private val mutableCurrentPage = MutableStateFlow(0)
    val currentPage = mutableCurrentPage.asStateFlow()

    private val mutableSortedByAsc = MutableStateFlow(false)
    val sortedByAsc = mutableSortedByAsc.asStateFlow()


    @OptIn(ExperimentalCoroutinesApi::class)
    private val chapterPageState = currentPage
        .combine(
            mutableSortedByAsc
        ) { page, asc -> page to asc }
            .flatMapLatest { (page, asc) ->
                chapterInfoRepository.getChapters(initialManga.id, page, asc)
            }


    val mangaViewStateUiState = combine(
        chapterPageState,
        chapterInfoRepository.getSavedChapters(initialManga.id),
        combineMangaChapterInfo(initialManga.id),
        chapterInfoRepository.loadingVolumeArtIds,
    ) { chapterState, chapters, mangaFull, loadingIds ->
        MangaViewState(
            mangaState = mangaFull.domainManga?.let{ manga ->
                MangaState.Success(
                    loadingArt = manga.id in loadingIds,
                    volumeToArt = mangaFull.volumeImages?.mapKeys { (k, _) -> k.toIntOrNull() ?: 0 } ?: emptyMap(),
                    manga = manga
                )
            } ?: MangaState.Loading,
            chapterPageState = when(chapterState) {
                is Resource.Failure, Resource.Loading -> ChapterPageState.Loading
                is Resource.Success -> ChapterPageState.Success(
                    lastPage = chapterState.result.lastPage,
                    volumeToChapters = chapterState.result.chapters.groupBy { it.attributes.volume?.toIntOrNull() ?: 0 }
                        .mapValues { (_, v) ->
                            v.run {
                                val sorted = this
                                    .sortedBy { it.attributes.chapter?.toDoubleOrNull() ?: 0.0 }
                                    .map {
                                        DomainChapter(
                                            ChapterToChapterEntityMapper.map(
                                                it to chapters.find { s -> s.id == it.id }
                                            )
                                        ).also {
                                            if (it.downloaded) {
                                                Log.d("CombineMangaChapterInfo", "${it.id} downloaded")
                                            }
                                        }
                                    }
                                return@run if (!chapterState.result.sortedByAsc) { sorted.reversed() } else sorted
                            }
                        }
                )
            }
        )
    }
        .stateInUi(MangaViewState())

    fun bookmarkManga(id: String) = coroutineScope.launch {
        savedMangaRepository.bookmarkManga(id)
    }

    fun changeDirection() = coroutineScope.launch {
        mutableSortedByAsc.update { !it }
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
                initialManga.id
            )
        )
    }

    fun navigateToPage(page: Int) {
        if (page > 0 && (page <= ((mangaViewStateUiState.value.chapterPageState as? ChapterPageState.Success)?.lastPage ?: Int.MAX_VALUE))) {
            mutableCurrentPage.update { page - 1 }
        }
    }

    override fun onDispose() {
        super.onDispose()
        ProtectedResources.ids.remove(initialManga.id)
    }
}

data class MangaViewState(
    val chapterPageState: ChapterPageState = ChapterPageState.Loading,
    val mangaState: MangaState = MangaState.Loading
)

sealed class MangaState {
    object Loading: MangaState()
    data class Success(
        val loadingArt: Boolean,
        val volumeToArt: Map<Int, String>,
        val manga: DomainManga,
    ) : MangaState()
}

sealed class ChapterPageState(
    open val lastPage: Int = 0,
    open val volumeToChapters: Map<Int, List<DomainChapter>> = emptyMap()
) {

    object Loading: ChapterPageState()

    data class Success(
        override val lastPage: Int = 0,
        override val volumeToChapters: Map<Int, List<DomainChapter>> = emptyMap()
    ): ChapterPageState(lastPage, volumeToChapters)
}

