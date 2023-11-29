package io.silv.reader

import android.util.Log
import cafe.adriel.voyager.core.model.screenModelScope
import com.zhuinden.flowcombinetuplekt.combineTuple
import io.silv.common.model.Resource
import io.silv.data.chapter.ChapterEntityRepository
import io.silv.data.chapter.ChapterImageRepository
import io.silv.data.workers.chapters.ChapterDownloadWorker
import io.silv.datastore.UserSettingsStore
import io.silv.datastore.model.ReaderSettings
import io.silv.model.SavableChapter
import io.silv.model.SavableManga
import io.silv.network.model.chapter.Chapter
import io.silv.ui.EventScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MangaReaderSM(
    getCombinedSavableMangaWithChapters: io.silv.domain.GetCombinedSavableMangaWithChapters,
    private val chapterImageRepository: ChapterImageRepository,
    private val chapterRepository: ChapterEntityRepository,
    private val userSettingsStore: UserSettingsStore,
    mangaId: String,
    initialChapterId: String,
): EventScreenModel<MangaReaderEvent>() {

    private val chapterId = MutableStateFlow(initialChapterId)

    val readerSettings = userSettingsStore.observeReaderSettings().conflate()
        .onEach { Log.d("ReaderSettings", it.toString()) }
        .stateInUi(ReaderSettings())

    val chapterImageUrls = mutableListOf<String>()

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
                if (chapter.downloaded && chapter.id !in downloadingIds.map { it.first } && chapter.imageUris.isEmpty()) {
                    MangaReaderState.Failure("Unable to download images from this source.")
                } else {
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
                }
            } else {
                if (chapterImageUrls.isNotEmpty()) {
                    return@map MangaReaderState.Success(
                        manga = manga,
                        readerChapters = ReaderChapters(
                            prev = sortedChapters.getOrNull(sortedChapters.indexOf(chapter) - 1),
                            next = sortedChapters.getOrNull(sortedChapters.indexOf(chapter) + 1),
                            current = chapter,
                            chapterImages = chapterImageUrls
                        ),
                        chapters = sortedChapters,
                    )
                }

                chapterImageRepository.getChapterImages(chapterId)
                    .fold<Resource<Pair<Chapter, List<String>>>, MangaReaderState>(
                        MangaReaderState.Loading
                    ) { _, resource ->
                        when (resource) {
                            is Resource.Failure -> MangaReaderState.Failure(
                                resource.message
                            )
                           Resource.Loading -> MangaReaderState.Loading
                            is Resource.Success -> if (resource.result.second.isEmpty()) {
                                MangaReaderState.Failure("Unable to download images from this source.")
                            }else {
                                MangaReaderState.Success(
                                    manga = manga,
                                    readerChapters = ReaderChapters(
                                        prev = sortedChapters.getOrNull(sortedChapters.indexOf(chapter) - 1),
                                        next = sortedChapters.getOrNull(sortedChapters.indexOf(chapter) + 1),
                                        current = chapter,
                                        chapterImages = resource.result.second.also { chapterImageUrls.addAll(it) }
                                    ),
                                    chapters = sortedChapters,
                                )
                            }
                        }
                    }
            }
    }
        .stateInUi(MangaReaderState.Loading)


    fun goToNextChapter(chapter: SavableChapter) {
        screenModelScope.launch {
            mangaReaderState.value.success?.let {
                val idx = it.chapters.indexOf(chapter)
                it.chapters.getOrNull(idx + 1)?.let { next ->
                    chapterId.update { next.id }
                }
            }
        }
    }

    fun updateReaderSettings(readerSettings: ReaderSettings) {
        screenModelScope.launch {
            userSettingsStore.updateReaderSettings(readerSettings)
        }
    }

    fun goToPrevChapter(chapter: SavableChapter) {
        screenModelScope.launch {
            mangaReaderState.value.success?.let {
                val idx = it.chapters.indexOf(chapter)
                it.chapters.getOrNull(idx - 1)?.let { prev ->
                    chapterId.update { prev.id }
                }
            }
        }
    }

    fun goToChapter(id: String) {
        screenModelScope.launch {
            chapterId.update { id }
        }
    }

    fun updateChapterPage(page: Int, lastPage: Int) {
        screenModelScope.launch {
            chapterRepository.updateLastReadPage(chapterId.value, page, lastPage)
        }
    }

    fun bookmarkChapter(id: String) {
        screenModelScope.launch {
            chapterRepository.bookmarkChapter(id)
        }
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