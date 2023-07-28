package io.silv.amadeus.ui.screens.manga_filter

import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.repositorys.FilteredMangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MangaFilterSM(
    private val filteredMangaRepository: FilteredMangaRepository,
    private val savedMangaRepository: SavedMangaRepository,
    tagId: String
): AmadeusScreenModel<MangaFilterEvent>() {

    val filteredUiState = combine(
        filteredMangaRepository.getMangaResources(tagId),
        savedMangaRepository.getSavedMangas()
    ) { resources, saved ->
        resources.map { r ->
            DomainManga(r, saved.find { it.id == r.id })
        }
    }
        .stateInUi(emptyList())

    fun loadNextPage() = coroutineScope.launch {
        filteredMangaRepository.loadNextPage()
    }

    fun bookmarkManga(id: String) = coroutineScope.launch {
        savedMangaRepository.bookmarkManga(id)
    }
}

