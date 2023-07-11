package io.silv.amadeus.ui.screens.library

import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.repositorys.SavedMangaRepository
import kotlinx.coroutines.flow.map

class LibrarySM(
    savedMangaRepository: SavedMangaRepository
): AmadeusScreenModel<LibraryEvent>() {

    val mangaWithDownloadedChapters = savedMangaRepository
        .getSavedMangaWithChapters()
        .map { mangas ->
            mangas.mapNotNull { (manga, chapters) ->
                DomainManga(manga) to
                (chapters
                    .filter { it.chapterImages.isNotEmpty() }
                    .takeIf { it.isNotEmpty() }
                    ?.map { DomainChapter(it) }
                    ?: return@mapNotNull null)
            }
        }
        .stateInUi(emptyList())
}


sealed interface LibraryEvent