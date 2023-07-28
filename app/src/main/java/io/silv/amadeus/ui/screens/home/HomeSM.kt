package io.silv.amadeus.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.repositorys.PopularMangaRepository
import io.silv.manga.domain.repositorys.RecentMangaRepository
import io.silv.manga.domain.repositorys.ResourceQuery
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.SearchMangaRepository
import io.silv.manga.domain.repositorys.SeasonalMangaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeSM(
    private val searchMangaRepository: SearchMangaRepository,
    private val recentMangaRepository: RecentMangaRepository,
    private val popularMangaRepository: PopularMangaRepository,
    private val seasonalMangaRepository: SeasonalMangaRepository,
    private val savedMangaRepository: SavedMangaRepository,
): AmadeusScreenModel<HomeEvent>() {

    private val mutableSearchText = MutableStateFlow("")
    val searchText = mutableSearchText.asStateFlow()

    var loadingPopularManga by mutableStateOf(false)
        private set

    var loadingRecentManga by mutableStateOf(false)
        private set

    init {
        coroutineScope.launch {
            seasonalMangaRepository.refreshList()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val mangaSearchFlow = searchText
        .debounce(3000)
        .flatMapMerge { txt ->
            searchMangaRepository.getMangaResources(
                ResourceQuery(
                    title = txt,
                    includedTags = null,
                    excludedTags = null
                )
            )
        }

    val searchMangaUiState = combine(
        mangaSearchFlow,
        savedMangaRepository.getSavedMangas()
    ) { resources, saved ->
        resources.map {
            DomainManga(it, saved.find { manga -> manga.id == it.id })
        }
    }
        .stateInUi(emptyList())

    val popularMangaUiState = combine(
        popularMangaRepository.getMangaResources(),
        savedMangaRepository.getSavedMangas()
    ) { resources, saved ->
        resources.map {
            DomainManga(it, saved.find { manga -> manga.id == it.id })
        }
    }
        .stateInUi(emptyList())

    val recentMangaUiState = combine(
        recentMangaRepository.getMangaResources(),
        savedMangaRepository.getSavedMangas()
    ) { resources, saved ->
        resources.map {
            DomainManga(it, saved.find { manga -> manga.id == it.id })
        }
    }
        .stateInUi(emptyList())

    val seasonalMangaUiState = combine(
        seasonalMangaRepository.getMangaResources(),
        savedMangaRepository.getSavedMangas()
    ) { resources, saved ->
        resources.map {
            DomainManga(it, saved.find { manga -> manga.id == it.id })
        }
    }
        .stateInUi(emptyList())

    fun bookmarkManga(mangaId: String) = coroutineScope.launch {
        savedMangaRepository.bookmarkManga(mangaId)
    }

    fun searchTextChanged(query: String)  {
        mutableSearchText.update { query }
    }

    fun loadNextPopularPage() = coroutineScope.launch {
        loadingPopularManga = true
        popularMangaRepository.loadNextPage()
        loadingPopularManga = false
    }

    fun loadNextRecentPage() = coroutineScope.launch {
        loadingRecentManga = true
        recentMangaRepository.loadNextPage()
        loadingRecentManga = false
    }
}

