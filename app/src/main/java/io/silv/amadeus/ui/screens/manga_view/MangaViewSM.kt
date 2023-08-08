package io.silv.amadeus.ui.screens.manga_view

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.amadeus.ui.shared.Language
import io.silv.manga.domain.models.SavableChapter
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.base.ProtectedResources
import io.silv.manga.domain.usecase.GetCombinedSavableMangaWithChapters
import io.silv.manga.local.workers.ChapterDeletionWorker
import io.silv.manga.local.workers.ChapterDeletionWorkerTag
import io.silv.manga.local.workers.ChapterDownloadWorker
import io.silv.manga.local.workers.ChapterDownloadWorkerTag
import io.silv.manga.sync.anyRunning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MangaViewSM(
    getCombinedSavableMangaWithChapters: GetCombinedSavableMangaWithChapters,
    private val savedMangaRepository: SavedMangaRepository,
    private val workManager: WorkManager,
    private val initialManga: SavableManga
): AmadeusScreenModel<MangaViewEvent>() {

    init {
        ProtectedResources.ids.add(initialManga.id)
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


    val mangaViewStateUiState = getCombinedSavableMangaWithChapters(initialManga.id)
        .map { combinedSavableMangaWithChapters ->
            combinedSavableMangaWithChapters.savableManga?.let {
                MangaViewState.Success(
                    loadingArt = false,
                    manga = it,
                    volumeToArt = it.volumeToCoverArtUrl.mapKeys { (k, v)-> k.toIntOrNull() ?: 0  },
                    chapters = combinedSavableMangaWithChapters.chapters.map { entity ->
                        SavableChapter(entity)
                    }
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

    fun languageChanged(language: Language)  {
        mutableLanguageList.update {
            if (language.code in it) it - language.code
            else it + language.code
        }
    }


    override fun onDispose() {
        super.onDispose()
        ProtectedResources.ids.remove(initialManga.id)
    }
}

sealed class MangaViewState(
    open val manga: SavableManga
) {
    data class Loading(override val manga: SavableManga) : MangaViewState(manga)
    data class Success(
        val loadingArt: Boolean,
        val volumeToArt: Map<Int, String>,
        override val manga: SavableManga,
        val chapters: List<SavableChapter>
    ) : MangaViewState(manga) {
        val volumeToChapter: Map<Int, List<SavableChapter>>
            get() = this.chapters.groupBy { it.volume?.toIntOrNull() ?: 0 }
                .mapValues {(k, v) ->
                    v.sortedBy { it.chapter?.toDoubleOrNull() ?: 0.0 }
                }
    }

    val success: Success?
        get() = this as? Success
}

sealed interface MangaViewEvent {
    data class FailedToLoadVolumeArt(val message: String): MangaViewEvent
    data class FailedToLoadChapterList(val message: String): MangaViewEvent
}
