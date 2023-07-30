package io.silv.amadeus.ui.screens.saved

import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.local.entity.relations.MangaWithChapters
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SavedMangaSM(
    private val savedMangaRepository: SavedMangaRepository
): AmadeusScreenModel<SavedMangaEvent>() {

    val savedMangas = savedMangaRepository.getSavedMangaWithChapters()
        .map { value: List<MangaWithChapters> ->
            value.map { (manga, chapters) ->
               DomainManga(manga) to chapters.map { DomainChapter(it) }
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