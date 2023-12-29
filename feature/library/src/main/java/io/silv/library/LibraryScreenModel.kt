package io.silv.library

import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.model.UpdateType
import io.silv.data.download.DownloadManager
import io.silv.domain.chapter.interactor.ChapterHandler
import io.silv.domain.chapter.interactor.GetBookmarkedChapters
import io.silv.domain.chapter.model.toResource
import io.silv.domain.manga.interactor.GetLibraryMangaWithChapters
import io.silv.domain.manga.model.toResource
import io.silv.domain.manga.repository.MangaUpdateRepository
import io.silv.ui.EventStateScreenModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class LibraryScreenModel(
    getBookmarkedChapters: GetBookmarkedChapters,
    getSavedMangaWithChaptersList: GetLibraryMangaWithChapters,
    mangaUpdateRepository: MangaUpdateRepository,
    private val downloadManager: DownloadManager,
    private val chapterHandler: ChapterHandler,
) : EventStateScreenModel<LibraryEvent, LibraryState>(LibraryState()) {


    init {
        getSavedMangaWithChaptersList.subscribe()
            .onEach { list ->
                mutableState.update {
                    it.copy(
                        libraryManga = list.map { (manga, chapters) ->
                            LibraryManga(
                                savableManga = manga,
                                chapters = chapters
                            )
                        }
                            .toImmutableList()
                    )
                }
            }
            .launchIn(screenModelScope)

        mangaUpdateRepository.observeAllUpdates().onEach { updateList ->
            updateList.mapNotNull { update ->
                when (update.updateType) {
                    UpdateType.Volume, UpdateType.Chapter ->
                        Update.Chapter(
                            chapterId = update.manga.latestUploadedChapter ?: return@mapNotNull null,
                            manga = update.manga,
                        )
                    UpdateType.Other -> null
                }
            }.let { updates ->
                mutableState.update { it.copy(updates = updates.toImmutableList()) }
            }
        }
            .launchIn(screenModelScope)

        getBookmarkedChapters.subscribe()
            .combine(downloadManager.queueState){ x, y -> x to y }
            .onEach { (bookmarked, downloads) ->
                val bookmarkedChapters = bookmarked.map { chapter ->
                    LibraryChapter(
                        chapter = chapter,
                        download = downloads.firstOrNull { it.chapter.id == chapter.id }
                    )
                }
                    .toImmutableList()

                withContext(Dispatchers.Main) {
                    mutableState.update { it.copy(bookmarkedChapters = bookmarkedChapters) }
                }
            }
            .launchIn(screenModelScope)
    }


    fun deleteChapterImages(chapterIds: List<String>, mangaId: String) =
        screenModelScope.launch {

            val libraryManga = state.value.libraryManga.find { it.savableManga.id == mangaId } ?: return@launch
            val chapters = libraryManga.chapters.filter { it.id in chapterIds }.ifEmpty { return@launch }

            downloadManager.deleteChapters(
                chapters = chapters.map { it.toResource() },
                manga = libraryManga.savableManga.toResource()
            )
        }

    fun downloadChapterImages(
        chapterIds: List<String>,
        mangaId: String,
    ) = screenModelScope.launch {
        val libraryManga = state.value.libraryManga.find { it.savableManga.id == mangaId } ?: return@launch
        val chapters = libraryManga.chapters.filter { it.id in chapterIds }.ifEmpty { return@launch }

        downloadManager.downloadChapters(
            chapters = chapters.map { it.toResource() },
            manga = libraryManga.savableManga.toResource()
        )
    }

    fun changeChapterBookmarked(id: String) {
        screenModelScope.launch {
            chapterHandler.toggleChapterBookmarked(id)
        }
    }

    fun changeChapterReadStatus(id: String) {
        screenModelScope.launch {
            chapterHandler.toggleReadOrUnread(id)
                .onSuccess {
                    it?.let {
                        mutableEvents.send(
                            LibraryEvent.ReadStatusChanged(id, it.read),
                        )
                    }
                }
        }
    }
}
