package io.silv.amadeus.ui.screens.saved

import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.local.entity.relations.MangaWithChapters
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

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
        it.filter { (manga, _) -> manga.progressState == ProgressState.Reading }
    }
        .stateInUi(emptyList())
}

sealed interface SavedMangaEvent