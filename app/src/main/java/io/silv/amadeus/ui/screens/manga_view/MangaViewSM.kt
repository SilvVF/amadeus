package io.silv.amadeus.ui.screens.manga_view

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.ktor_response_mapper.message
import io.silv.ktor_response_mapper.suspendOnFailure
import io.silv.ktor_response_mapper.suspendOnSuccess
import io.silv.manga.domain.models.SavableChapter
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.base.ProtectedResources
import io.silv.manga.domain.usecase.GetCombinedSavableMangaWithChapters
import io.silv.manga.domain.usecase.GetMangaStatisticsById
import io.silv.manga.domain.usecase.MangaStats
import io.silv.manga.local.workers.ChapterDeletionWorker
import io.silv.manga.local.workers.ChapterDeletionWorkerTag
import io.silv.manga.local.workers.ChapterDownloadWorker
import io.silv.manga.local.workers.ChapterDownloadWorkerTag
import io.silv.manga.sync.anyRunning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class MangaViewSM(
    getCombinedSavableMangaWithChapters: GetCombinedSavableMangaWithChapters,
    getMangaStatisticsById: GetMangaStatisticsById,
    private val savedMangaRepository: SavedMangaRepository,
    private val workManager: WorkManager,
    private val initialManga: SavableManga
): AmadeusScreenModel<MangaViewEvent>() {

    init {
        ProtectedResources.ids.add(initialManga.id)
    }

    val statsUiState = flow {
        emit(StatsUiState(loading = true))
        getMangaStatisticsById(initialManga.id)
            .suspendOnFailure { emit(StatsUiState(error = message())) }
            .suspendOnSuccess { emit(StatsUiState(data = data)) }
    }
        .stateInUi(StatsUiState(loading = true))


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

    private val mutableSortedByAsc = MutableStateFlow(false)

    val mangaViewStateUiState = combine(
        getCombinedSavableMangaWithChapters(initialManga.id),
        mutableSortedByAsc
    ) { combinedSavableMangaWithChapters, asc ->
            combinedSavableMangaWithChapters.savableManga?.let {
                val chapters = combinedSavableMangaWithChapters
                        .chapters.map { SavableChapter(it) }
                MangaViewState.Success(
                    loadingArt = false,
                    manga = it,
                    volumeToArt = it.volumeToCoverArtUrl.mapKeys { (k, v)-> k.toIntOrNull() ?: 0  },
                    volumeToChapters = chapters
                        .groupByVolumeSorted(asc),
                    chapters = chapters,
                    asc = asc
                )
            } ?: MangaViewState.Loading(initialManga)
    }
        .stateInUi(MangaViewState.Loading(initialManga))

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


    override fun onDispose() {
        super.onDispose()
        ProtectedResources.ids.remove(initialManga.id)
    }
}

fun List<SavableChapter>.groupByVolumeSorted(asc: Boolean) =
    this.groupBy { it.volume }
        .mapKeys { (k, v) -> if(k == -1) Int.MAX_VALUE else k }
        .toSortedMap { v1, v2 -> if(asc) v1 - v2 else v2 - v1 }
        .mapValues { (k, v) ->
            if (asc) v.sortedBy { it.chapter }
            else v.sortedByDescending { it.chapter }
        }
        .mapKeys { (k, v) -> if(k ==  Int.MAX_VALUE ) -1 else k }
        .toList()

sealed class MangaViewState(
    open val manga: SavableManga,
    val sortedByAscending: Boolean,
    open val chapters: List<SavableChapter>,
) {
    data class Loading(override val manga: SavableManga) : MangaViewState(manga, true, emptyList())
    data class Success(
        val loadingArt: Boolean,
        val volumeToArt: Map<Int, String>,
        override val manga: SavableManga,
        val volumeToChapters: List<Pair<Int, List<SavableChapter>>>,
        val asc: Boolean,
        override val chapters: List<SavableChapter>,
    ) : MangaViewState(manga, asc, chapters)


    val success: Success?
        get() = this as? Success
}

data class StatsUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val data: MangaStats = MangaStats()
)

sealed interface MangaViewEvent {
    data class FailedToLoadVolumeArt(val message: String): MangaViewEvent
    data class FailedToLoadChapterList(val message: String): MangaViewEvent
}
