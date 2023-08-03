package io.silv.amadeus.ui.screens.manga_reader

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.SavableChapter
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.local.entity.relations.SavedMangaWithChapters
import io.silv.manga.local.workers.ChapterDownloadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MangaReaderSM(
    private val workManager: WorkManager,
    private val savedMangaRepository: SavedMangaRepository,
    private val mangaId: String,
    initialChapterId: String,
): AmadeusScreenModel<MangaReaderEvent>() {

    private val chapterId = MutableStateFlow(initialChapterId)

    private var saveMangaJob: Job? = null

    val mangaWithChapters = combineMangaWithChapter(
        chapterId,
        savedMangaRepository.getSavedMangaWithChapter(mangaId),
        mangaNotFound = {
            if (saveMangaJob == null) {
                saveMangaJob = CoroutineScope(Dispatchers.IO).launch {
                    savedMangaRepository.saveManga(mangaId)
                }
            }
        },
        chapterNotFound = {},
        notEnoughImages = {
            loadMangaImages()
        }
    )
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

private fun combineMangaWithChapter(
    chapterId: Flow<String>,
    manga: Flow<SavedMangaWithChapters?>,
    mangaNotFound: suspend () -> Unit,
    chapterNotFound: suspend () -> Unit,
    notEnoughImages: suspend (prev: Int) -> Unit
) = combine(
    chapterId,
    manga
) { cid, mangaWithChapters ->
    if (mangaWithChapters == null) {
        mangaNotFound()
    }
    val chapter = mangaWithChapters?.chapters?.find { it.id == cid }
    if(chapter == null) {
        chapterNotFound()
    }
    val images = chapter?.chapterImages?.takeIf { it.isNotEmpty() }
    if (images != null) {
        if (chapter.pages < images.size) {
            notEnoughImages(images.size)
        }
        MangaReaderState.Success(
            SavableManga(mangaWithChapters.manga),
            SavableChapter(chapter),
            mangaWithChapters.chapters.map { SavableChapter(it) },
            images
        )
    } else {
        notEnoughImages(0)
        MangaReaderState.Loading
    }
}

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