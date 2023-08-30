package io.silv.amadeus.ui.screens.library

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.manga_usecase.GetSavedMangaWithChaptersList
import io.silv.amadeus.types.SavableChapter
import io.silv.amadeus.types.SavableManga
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.local.workers.ChapterDeletionWorker
import io.silv.manga.local.workers.ChapterDeletionWorkerTag
import io.silv.manga.local.workers.ChapterDownloadWorker
import io.silv.manga.local.workers.ChapterDownloadWorkerTag
import io.silv.manga.repositorys.chapter.ChapterEntityRepository
import io.silv.manga.sync.anyRunning
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LibrarySM(
    chapterEntityRepository: ChapterEntityRepository,
    getSavedMangaWithChaptersList: GetSavedMangaWithChaptersList,
    private val workManager: WorkManager,
): AmadeusScreenModel<LibraryEvent>() {

    val downloadingOrDeleting = combine(
        workManager.getWorkInfosByTagFlow(ChapterDownloadWorkerTag)
            .map { it.anyRunning() },
        workManager.getWorkInfosByTagFlow(ChapterDeletionWorkerTag)
            .map { it.anyRunning() },
        ChapterDownloadWorker.downloadingIdToProgress
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

    fun deleteChapterImages(chapterIds: List<String>) = coroutineScope.launch {
        workManager.enqueue(
            ChapterDeletionWorker.deletionWorkRequest(chapterIds)
        )
    }

    fun downloadChapterImages(chapterIds: List<String>, mangaId: String) = coroutineScope.launch {
        workManager.enqueueUniqueWork(
            chapterIds.toString(),
            ExistingWorkPolicy.KEEP,
            ChapterDownloadWorker.downloadWorkRequest(
                chapterIds,
                mangaId
            )
        )
    }
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

sealed interface LibraryEvent