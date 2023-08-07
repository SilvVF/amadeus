package io.silv.amadeus.ui.screens.manga_view

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.amadeus.ui.shared.Language
import io.silv.manga.domain.ChapterToChapterEntityMapper
import io.silv.manga.domain.models.SavableChapter
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.base.ProtectedResources
import io.silv.manga.domain.repositorys.base.Resource
import io.silv.manga.domain.repositorys.chapter.ChapterInfoResponse
import io.silv.manga.domain.repositorys.chapter.ChapterListRepository
import io.silv.manga.domain.usecase.GetCombinedSavableMangaWithChapters
import io.silv.manga.local.entity.ChapterEntity
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
    getCombinedSavableMangaWithChapters: GetCombinedSavableMangaWithChapters,
    private val savedMangaRepository: SavedMangaRepository,
    private val workManager: WorkManager,
    private val initialManga: SavableManga
): AmadeusScreenModel<MangaViewEvent>() {

    init {
        ProtectedResources.ids.add(initialManga.id)
        coroutineScope.launch {
            chapterInfoRepository.loadVolumeArt(initialManga.id)
        }
    }

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

    private val mutableLanguageList = MutableStateFlow(listOf("en"))
    val languageList = mutableLanguageList.asStateFlow()


    @OptIn(ExperimentalCoroutinesApi::class)
    private val observeChaptersForPage = combine(
        mutableCurrentPage,
        mutableSortedByAsc,
        mutableLanguageList
    ) { page, asc, langs -> Triple(page, asc, langs) }
        .flatMapLatest { (page, asc, langs) ->
            chapterInfoRepository.observeChapters(initialManga.id, page, asc, langs)
        }


    val mangaViewStateUiState = combine(
        observeChaptersForPage,
        getCombinedSavableMangaWithChapters(initialManga.id),
        chapterInfoRepository.loadingVolumeArtIds,
    ) { chaptersList, combinedSavableMangaWithChapters, loadingVolumeArtIds ->
        val (savableManga, chapterEntities) = combinedSavableMangaWithChapters
        MangaViewUiState(
            mangaState = MangaState.Success(
                    loadingArt = savableManga.id in loadingVolumeArtIds,
                    volumeToArt = savableManga.volumeToCoverArtUrl.mapKeys { (k, _) -> k.toIntOrNull() ?: 0 },
                    manga = savableManga
            ),
            chapterPageState = when(chaptersList) {
                is Resource.Failure -> {
                    mutableEvents.send(
                        MangaViewEvent
                            .FailedToLoadChapterList(
                                "Failed to load chapters list check internet connection"
                            )
                    )
                    ChapterPageState.Loading
                }
                is Resource.Loading -> ChapterPageState.Loading
                is Resource.Success -> ChapterPageState.Success(
                    lastPage = chaptersList.result.lastPage,
                    volumeToChapters = chaptersList.result
                        .toChapterPageState(chapterEntities)
                )
            }
        )
    }
        .stateInUi(MangaViewUiState(mangaState = MangaState.Loading(initialManga)))

    fun bookmarkManga(id: String) = coroutineScope.launch {
        savedMangaRepository.bookmarkManga(id)
    }

    fun changeDirection() = coroutineScope.launch {
        mutableSortedByAsc.update { !it }
    }

    fun deleteChapterImages(chapterIds: List<String>) = coroutineScope.launch {
        workManager.enqueue(
            ChapterDeletionWorker.deletionWorkRequest(chapterIds)
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

    fun languageChanged(language: Language)  {
        mutableLanguageList.update {
            if (language.code in it) it - language.code
            else it + language.code
        }
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

data class MangaViewUiState(
    val chapterPageState: ChapterPageState = ChapterPageState.Loading,
    val mangaState: MangaState
)

sealed class MangaState(
    open val manga: SavableManga
) {
    data class Loading(override val manga: SavableManga) : MangaState(manga)
    data class Success(
        val loadingArt: Boolean,
        val volumeToArt: Map<Int, String>,
        override val manga: SavableManga,
    ) : MangaState(manga)
}

fun ChapterInfoResponse.toChapterPageState(savedChapters: List<ChapterEntity>) =
    this.chapters.groupBy { it.attributes.volume?.toIntOrNull() ?: 0 }
        .mapValues { (_, v) ->
            v.run {
                val sorted = this
                    .sortedBy { it.attributes.chapter?.toDoubleOrNull() ?: 0.0 }
                    .map {
                        SavableChapter(
                            ChapterToChapterEntityMapper.map(
                                it to savedChapters.find { s -> s.id == it.id }
                            )
                        )
                    }
                return@run if (!this@toChapterPageState.sortedByAsc) { sorted.reversed() } else sorted
            }
        }

sealed interface MangaViewEvent {
    data class FailedToLoadVolumeArt(val message: String): MangaViewEvent
    data class FailedToLoadChapterList(val message: String): MangaViewEvent
}

sealed class ChapterPageState(
    open val lastPage: Int = 0,
) {

    object Loading: ChapterPageState()

    data class Success(
         override val lastPage: Int = 0,
         val volumeToChapters: Map<Int, List<SavableChapter>> = emptyMap()
    ): ChapterPageState(lastPage)
}

