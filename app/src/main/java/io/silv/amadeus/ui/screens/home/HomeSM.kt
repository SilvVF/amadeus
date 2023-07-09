package io.silv.amadeus.ui.screens.home

import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.repositorys.MangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HomeSM(
    private val mangaRepository: MangaRepository,
    private val savedMangaRepository: SavedMangaRepository,
): AmadeusScreenModel<HomeEvent>() {

    val isSyncing = MutableStateFlow(false)

    val mangaUiState = combine(
        mangaRepository.getMangaResources(),
        savedMangaRepository.getSavedMangas()
    ) { resources, saved ->
        resources.map { resource ->
            DomainManga(resource, saved.find { it.id ==  resource.id})
        }
    }
        .stateInUi(emptyList())

    fun bookmarkManga(mangaId: String) = coroutineScope.launch {
        savedMangaRepository.bookmarkManga(mangaId)
    }

    fun goToNextPage() {
        coroutineScope.launch {
            isSyncing.emit(true)
            mangaRepository.loadNextPage()
            isSyncing.emit(false)
        }
    }
}

