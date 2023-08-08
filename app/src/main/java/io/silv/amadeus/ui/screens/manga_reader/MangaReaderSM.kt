package io.silv.amadeus.ui.screens.manga_reader

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.SavableChapter
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.chapter.ChapterImageRepository
import io.silv.manga.domain.usecase.GetCombinedSavableMangaWithChapters
import io.silv.manga.local.workers.ChapterDownloadWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MangaReaderSM(
    private val workManager: WorkManager,
    private val savedMangaRepository: SavedMangaRepository,
    private val getCombinedSavableMangaWithChapters: GetCombinedSavableMangaWithChapters,
    private val chapterImageRepository: ChapterImageRepository,
    private val mangaId: String,
    initialChapterId: String,
): AmadeusScreenModel<MangaReaderEvent>() {

    private val chapterId = MutableStateFlow(initialChapterId)

    @OptIn(ExperimentalCoroutinesApi::class)
    val mangaWithChapters = getCombinedSavableMangaWithChapters(mangaId)
        .combineToPair(chapterId)
        .flatMapLatest { (mangaWithChapters, id) ->
            flowOf(MangaReaderState.Loading)
        }
        .stateInUi(MangaReaderState.Loading)


    fun goToNextChapter(chapter: SavableChapter) = coroutineScope.launch {
        chapterId.update { id ->
            val chapterNumber = chapter.chapter?.toIntOrNull() ?: 0
            (mangaWithChapters.value as? MangaReaderState.Success)?.let { state ->
                state.chapters.find {
                    it.chapter?.toIntOrNull() == chapterNumber + 1
                }?.id ?: id
            } ?: id
        }
    }

    fun goToPrevChapter(chapter: SavableChapter) = coroutineScope.launch {
        chapterId.update { id ->
            val chapterNumber = chapter.chapter?.toIntOrNull() ?: 0
            (mangaWithChapters.value as? MangaReaderState.Success)?.let { state ->
                state.chapters.find {
                    it.chapter?.toIntOrNull() == chapterNumber - 1
                }?.id ?: id
            } ?: id
        }
    }

    fun goToChapter(id: String) = coroutineScope.launch {
        chapterId.emit(id)
    }

    fun updateChapterPage(page: Int) = coroutineScope.launch {
        savedMangaRepository.updateLastReadPage(mangaId, chapterId.value, page)
    }

    private fun loadMangaImages() = coroutineScope.launch {
        workManager.enqueueUniqueWork(
            chapterId.value,
            ExistingWorkPolicy.KEEP,
            ChapterDownloadWorker.downloadWorkRequest(
                listOf(chapterId.value),
                mangaId
            )
        )
    }

}

fun <T, V> Flow<T>.combineToPair(other: Flow<V>): Flow<Pair<T, V>> = this.combine(other) { t, v -> Pair(t, v) }

sealed interface MangaReaderEvent

sealed class MangaReaderState {
    object Loading: MangaReaderState()
    data class Success(
        val manga: SavableManga,
        val chapter: SavableChapter,
        val chapters: List<SavableChapter>,
        val pages: List<String> = emptyList()
    ): MangaReaderState()
}