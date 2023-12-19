package io.silv.library

import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.model.ProgressState
import io.silv.common.model.UpdateType
import io.silv.data.chapter.ChapterRepository
import io.silv.data.manga.MangaUpdateRepository
import io.silv.domain.manga.GetLibraryMangaWithChapters
import io.silv.model.SavableChapter
import io.silv.model.SavableManga
import io.silv.ui.EventScreenModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LibrarySM(
    private val chapterEntityRepository: ChapterRepository,
    getSavedMangaWithChaptersList: GetLibraryMangaWithChapters,
    mangaUpdateRepository: MangaUpdateRepository,
) : EventScreenModel<LibraryEvent>() {
    val downloadingOrDeleting = flowOf(emptyList<Pair<String, Float>>())

    val bookmarkedChapters =
        chapterEntityRepository.observeChapters()
            .map { entities ->
                entities.filter { chapter -> chapter.bookmarked }
                    .map {
                        SavableChapter(it)
                    }
            }
            .stateInUi(emptyList())

    val mangaWithDownloadedChapters =
        getSavedMangaWithChaptersList.subscribe()
            .map { list ->
                list.map { (manga, chapters) ->
                    LibraryManga(
                        chapters = chapters,
                        savableManga = manga,
                    )
                }
            }
            .stateInUi(emptyList())

    val updates =
        mangaUpdateRepository.observeAllUpdates().map { updates ->
            updates.mapNotNull { updateWithManga ->
                val (update, manga) = updateWithManga
                when (update.updateType) {
                    UpdateType.Volume, UpdateType.Chapter ->
                        Update.Chapter(
                            chapterId = manga.latestUploadedChapter ?: return@mapNotNull null,
                            SavableManga(manga),
                        )
                    UpdateType.Other -> null
                }
            }
        }
            .stateInUi(emptyList())

    fun deleteChapterImages(chapterIds: List<String>) =
        screenModelScope.launch {
        }

    fun downloadChapterImages(
        chapterIds: List<String>,
        mangaId: String,
    ) = screenModelScope.launch {
    }

    fun changeChapterBookmarked(id: String) =
        screenModelScope.launch {
            var new = false
            chapterEntityRepository.updateChapter(id) { entity ->
                entity.copy(
                    bookmarked = !entity.bookmarked.also { new = it },
                )
            }
            mutableEvents.send(
                LibraryEvent.BookmarkStatusChanged(id, new),
            )
        }

    fun changeChapterReadStatus(id: String) =
        screenModelScope.launch {
            var new = false
            chapterEntityRepository.updateChapter(id) { entity ->
                entity.copy(
                    progressState =
                    when (entity.progressState) {
                        ProgressState.Finished -> ProgressState.NotStarted.also { new = false }
                        ProgressState.NotStarted, ProgressState.Reading -> ProgressState.Finished.also { new = true }
                    },
                )
            }
            mutableEvents.send(
                LibraryEvent.ReadStatusChanged(id, new),
            )
        }
}
