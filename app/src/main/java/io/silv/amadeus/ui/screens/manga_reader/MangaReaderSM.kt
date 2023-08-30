package io.silv.amadeus.ui.screens.manga_reader

import android.util.Log
import cafe.adriel.voyager.core.model.coroutineScope
import com.zhuinden.flowcombinetuplekt.combineTuple
import io.silv.amadeus.data.ReaderSettings
import io.silv.amadeus.data.UserSettingsStore
import io.silv.amadeus.manga_usecase.GetCombinedSavableMangaWithChapters
import io.silv.amadeus.types.SavableChapter
import io.silv.amadeus.types.SavableManga
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.local.workers.ChapterDownloadWorker
import io.silv.manga.network.mangadex.models.chapter.Chapter
import io.silv.manga.repositorys.Resource
import io.silv.manga.repositorys.chapter.ChapterEntityRepository
import io.silv.manga.repositorys.chapter.ChapterImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MangaReaderSM(
    getCombinedSavableMangaWithChapters: GetCombinedSavableMangaWithChapters,
    private val chapterImageRepository: ChapterImageRepository,
    private val chapterRepository: ChapterEntityRepository,
    private val userSettingsStore: UserSettingsStore,
    mangaId: String,
    initialChapterId: String,
): AmadeusScreenModel<MangaReaderEvent>() {

    private val chapterId = MutableStateFlow(initialChapterId)

    val readerSettings = userSettingsStore.observeReaderSettings().conflate()
        .onEach { Log.d("ReaderSettings", it.toString()) }
        .stateInUi(ReaderSettings())

    val mangaReaderState = combineTuple(
        chapterId,
        getCombinedSavableMangaWithChapters(mangaId),
        ChapterDownloadWorker.downloadingIdToProgress.asStateFlow()
    ).map { (chapterId, savableWithChapters, downloadingIds) ->
            val (manga, chapters) = savableWithChapters

            if (manga == null) { return@map MangaReaderState.Failure("manga not found") }

            val chapter = chapters
                .find { c -> c.id == chapterId }
                ?.let { SavableChapter(it) }
                ?: return@map MangaReaderState.Failure("chapter not found")

            val sortedChapters = chapters
                .map { SavableChapter(it) }
                .sortedBy { it.chapter }

            if (chapter.downloaded || chapter.id in downloadingIds.map { it.first }) {
                MangaReaderState.Success(
                    manga = manga,
                    readerChapters = ReaderChapters(
                        prev = sortedChapters.getOrNull(sortedChapters.indexOf(chapter) - 1),
                        next = sortedChapters.getOrNull(sortedChapters.indexOf(chapter) + 1),
                        current = chapter,
                        chapterImages = chapter.imageUris
                    ),
                    chapters = sortedChapters,
                )
            } else {
                chapterImageRepository.getChapterImages(chapterId)
                    .fold<Resource<Pair<Chapter, List<String>>>, MangaReaderState>(MangaReaderState.Loading) { _, resource ->
                    when (resource) {
                        is Resource.Failure -> MangaReaderState.Failure(resource.message)
                        Resource.Loading -> MangaReaderState.Loading
                        is Resource.Success -> MangaReaderState.Success(
                            manga = manga,
                            readerChapters = ReaderChapters(
                                prev = sortedChapters.getOrNull(sortedChapters.indexOf(chapter) - 1),
                                next = sortedChapters.getOrNull(sortedChapters.indexOf(chapter) + 1),
                                current = chapter,
                                chapterImages = resource.result.second
                            ),
                            chapters = sortedChapters,
                        )
                    }
                }
            }
    }
        .stateInUi(MangaReaderState.Loading)


    fun goToNextChapter(chapter: SavableChapter) = coroutineScope.launch {
        mangaReaderState.value.success?.let {
            val idx = it.chapters.indexOf(chapter)
            it.chapters.getOrNull(idx + 1)?.let { next ->
                chapterId.update { next.id }
            }
        }
    }

    fun updateReaderSettings(readerSettings: ReaderSettings) = coroutineScope.launch {
        userSettingsStore.updateReaderSettings(readerSettings)
    }

    fun goToPrevChapter(chapter: SavableChapter) = coroutineScope.launch {
        mangaReaderState.value.success?.let {
            val idx = it.chapters.indexOf(chapter)
            it.chapters.getOrNull(idx - 1)?.let { prev ->
                chapterId.update { prev.id }
            }
        }
    }

    fun goToChapter(id: String) = coroutineScope.launch {
        chapterId.update { id }
    }

    fun updateChapterPage(page: Int, lastPage: Int) = coroutineScope.launch {
        chapterRepository.updateLastReadPage(chapterId.value, page, lastPage)
    }

    fun bookmarkChapter(id: String) = coroutineScope.launch {
        chapterRepository.bookmarkChapter(id)
    }
}

data class ReaderChapters(
    val prev: SavableChapter?,
    val next: SavableChapter?,
    val current: SavableChapter,
    val chapterImages: List<String>,
) {

    val hasPrev = prev != null

    val hasNext = next != null
}

sealed interface MangaReaderEvent

sealed class MangaReaderState {

    object Loading: MangaReaderState()

    data class Success(
        val manga: SavableManga,
        val chapters: List<SavableChapter>,
        val readerChapters: ReaderChapters
    ): MangaReaderState()

    data class Failure(val message: String? = null): MangaReaderState()

    val success: Success?
        get() = this as? Success
}