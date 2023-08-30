package io.silv.amadeus.ui.screens.library

import io.silv.amadeus.manga_usecase.GetSavedMangaWithChaptersList
import io.silv.amadeus.types.SavableChapter
import io.silv.amadeus.types.SavableManga
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.repositorys.chapter.ChapterEntityRepository
import kotlinx.coroutines.flow.map

class LibrarySM(
    chapterEntityRepository: ChapterEntityRepository,
    getSavedMangaWithChaptersList: GetSavedMangaWithChaptersList
): AmadeusScreenModel<LibraryEvent>() {

    val bookmarkedChapters = chapterEntityRepository.getAllChapters()
        .map { entities ->
            entities.filter { chapter -> chapter.bookmarked }
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