package io.silv.amadeus.ui.screens.library

import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.SavableChapter
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.SavedMangaRepository
import kotlinx.coroutines.flow.map

class LibrarySM(
    savedMangaRepository: SavedMangaRepository
): AmadeusScreenModel<LibraryEvent>() {

    val mangaWithDownloadedChapters = savedMangaRepository
        .getSavedMangaWithChapters()
        .map { mangas ->
            mangas.mapNotNull { (manga, chapters) ->
                SavableManga(manga) to
                (chapters
                    .filter { it.chapterImages.isNotEmpty() }
                    .takeIf { it.isNotEmpty() }
                    ?.map { SavableChapter(it) }
                    ?: return@mapNotNull null)
            }
        }
        .stateInUi(emptyList())
}


sealed interface LibraryEvent