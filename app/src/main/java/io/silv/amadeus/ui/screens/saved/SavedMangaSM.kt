package io.silv.amadeus.ui.screens.saved

import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.SavableChapter
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.local.entity.relations.SavedMangaWithChapters
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SavedMangaSM(
    private val savedMangaRepository: SavedMangaRepository
): AmadeusScreenModel<SavedMangaEvent>() {

    val savedMangas = savedMangaRepository.getSavedMangaWithChapters()
        .map { value: List<SavedMangaWithChapters> ->
            value.map { (manga, chapters) ->
               SavableManga(manga) to chapters.map { SavableChapter(it) }
            }
        }
        .stateInUi(emptyList())

    val bookmarkedMangas = savedMangas.map {
        it.filter { (manga, _) -> manga.bookmarked  }
    }
        .stateInUi(emptyList())

    val continueReading = savedMangas.map {
        it.filter { (manga, _) -> manga.chapterToLastReadPage.values.any { page -> page != 0 } }
    }
        .stateInUi(emptyList())

    fun bookmarkManga(mangaId: String) = coroutineScope.launch {
        savedMangaRepository.bookmarkManga(mangaId)
    }
}

sealed interface SavedMangaEvent