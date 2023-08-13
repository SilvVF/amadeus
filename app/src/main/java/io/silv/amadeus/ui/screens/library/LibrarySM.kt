package io.silv.amadeus.ui.screens.library

import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.SavableChapter
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.local.entity.ProgressState
import kotlinx.coroutines.flow.map

class LibrarySM(
    savedMangaRepository: SavedMangaRepository
): AmadeusScreenModel<LibraryEvent>() {

    val mangaWithDownloadedChapters = savedMangaRepository
        .getSavedMangaWithChapters()
        .map { list ->
            list.map { (manga, chapters) ->
                LibraryManga(
                    chapters = chapters.map { SavableChapter(it) },
                    savableManga = SavableManga(manga)
                )
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