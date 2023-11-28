package io.silv.amadeus.ui.screens.library

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.common.model.ProgressState
import io.silv.common.model.UpdateType
import io.silv.data.chapter.ChapterEntityRepository
import io.silv.data.manga.MangaUpdateRepository
import io.silv.domain.GetSavedMangaWithChaptersList
import io.silv.model.SavableChapter
import io.silv.model.SavableManga
import io.silv.sync.anyRunning
import io.silv.ui.EventScreenModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LibrarySM(
    private val chapterEntityRepository: ChapterEntityRepository,
    getSavedMangaWithChaptersList: GetSavedMangaWithChaptersList,
    private val mangaUpdateRepository: MangaUpdateRepository,
    private val workManager: WorkManager,
): EventScreenModel<LibraryEvent>() {

    val downloadingOrDeleting = combine(
        workManager.getWorkInfosByTagFlow(io.silv.data.workers.chapters.ChapterDownloadWorkerTag)
            .map { it.anyRunning() },
        workManager.getWorkInfosByTagFlow(io.silv.data.workers.chapters.ChapterDeletionWorkerTag)
            .map { it.anyRunning() },
        io.silv.data.workers.chapters.ChapterDownloadWorker.downloadingIdToProgress
    ) { downloading, deleting, idsToProgress ->
        if (downloading || deleting) {
            idsToProgress
        } else {
            emptyList()
        }
    }
        .stateInUi(emptyList())

    val bookmarkedChapters = chapterEntityRepository.getAllChapters()
        .map { entities ->
            entities.filter { chapter -> chapter.bookmarked }
                .map {
                    SavableChapter(it)
                }
        }
        .stateInUi(emptyList())

    val mangaWithDownloadedChapters = getSavedMangaWithChaptersList()
        .map { list ->
            list.mapNotNull { (manga, chapters) ->
                manga?.let { saved ->
                    LibraryManga(
                        chapters = chapters.map { SavableChapter(it) },
                        savableManga = saved
                    )
                }
            }
        }
        .stateInUi(emptyList())

    val updates = mangaUpdateRepository.observeAllUpdates().map { updates ->
        updates.mapNotNull { updateWithManga ->
           val (update, manga) = updateWithManga
            when (update.updateType) {
                UpdateType.Volume, UpdateType.Chapter -> Update.Chapter(
                    chapterId = manga.latestUploadedChapter ?: return@mapNotNull null,
                    SavableManga(manga)
                )
                UpdateType.Other -> null
            }
        }
    }
        .stateInUi(emptyList())

    fun deleteChapterImages(chapterIds: List<String>) = coroutineScope.launch {
        workManager.enqueue(
            io.silv.data.workers.chapters.ChapterDeletionWorker.deletionWorkRequest(chapterIds)
        )
    }

    fun downloadChapterImages(chapterIds: List<String>, mangaId: String) = coroutineScope.launch {
        workManager.enqueueUniqueWork(
            chapterIds.toString(),
            ExistingWorkPolicy.KEEP,
            io.silv.data.workers.chapters.ChapterDownloadWorker.downloadWorkRequest(
                chapterIds,
                mangaId
            )
        )
    }

    fun changeChapterBookmarked(id: String) = coroutineScope.launch {
        var new = false
        chapterEntityRepository.updateChapter(id) { entity ->
            entity.copy(
                bookmarked = !entity.bookmarked.also { new = it }
            )
        }
        mutableEvents.send(
            LibraryEvent.BookmarkStatusChanged(id, new)
        )
    }

    fun changeChapterReadStatus(id: String) = coroutineScope.launch {
        var new = false
        chapterEntityRepository.updateChapter(id) { entity ->
            entity.copy(
                progressState = when(entity.progressState) {
                    ProgressState.Finished -> ProgressState.NotStarted.also { new = false }
                    ProgressState.NotStarted, ProgressState.Reading -> ProgressState.Finished.also { new = true }
                }
            )
        }
        mutableEvents.send(
            LibraryEvent.ReadStatusChanged(id, new)
        )
    }
}

sealed interface Update {
    data class Chapter(val chapterId: String, val manga: SavableManga): Update
    data class Volume(val chapterId: String, val manga: SavableManga): Update
}

data class LibraryManga(
    val savableManga: SavableManga,
    val chapters: List<SavableChapter>,
) {

    val unread: Int
        get() = chapters.count { it.progress != ProgressState.Finished }

    val lastReadChapter: SavableChapter?
        get() = chapters
            .filter { it.progress == ProgressState.Reading || it.progress == ProgressState.NotStarted }
            .minByOrNull { if (it.chapter != 1L) it.chapter else Long.MAX_VALUE }
            ?: chapters.firstOrNull()
}

sealed interface LibraryEvent {
    data class BookmarkStatusChanged(val id: String, val bookmarked: Boolean): LibraryEvent
    data class ReadStatusChanged(val id: String, val read: Boolean): LibraryEvent
}