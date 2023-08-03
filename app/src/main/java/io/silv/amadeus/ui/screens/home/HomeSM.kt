package io.silv.amadeus.ui.screens.home

import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.screens.search.SearchMangaUiState
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.DomainTag
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.PopularMangaRepository
import io.silv.manga.domain.repositorys.RecentMangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.SearchMangaRepository
import io.silv.manga.domain.repositorys.SearchMangaResourceQuery
import io.silv.manga.domain.repositorys.SeasonalMangaRepository
import io.silv.manga.domain.repositorys.base.LoadState
import io.silv.manga.domain.repositorys.base.toBool
import io.silv.manga.domain.repositorys.tags.TagRepository
import io.silv.manga.local.entity.Season
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeSM(
    private val recentMangaRepository: RecentMangaRepository,
    private val popularMangaRepository: PopularMangaRepository,
    seasonalMangaRepository: SeasonalMangaRepository,
    private val savedMangaRepository: SavedMangaRepository,
    private val searchMangaRepository: SearchMangaRepository,
    private val tagRepository: TagRepository
): AmadeusScreenModel<HomeEvent>() {

    private val mutableSearchQuery = MutableStateFlow("")
    val searchQuery = mutableSearchQuery.asStateFlow()

    val loadingPopularManga = popularMangaRepository.loadState
        .map(::toBool)
        .stateInUi(false)

    val loadingRecentManga = recentMangaRepository.loadState
        .map(::toBool)
        .stateInUi(false)


    val tagsUiState = tagRepository.allTags().map {
        it.map { tag ->
            DomainTag(tag)
        }
    }
        .stateInUi(emptyList())
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val mangaSearchFlow = searchQuery
        .debounce { 1000L }
        .flatMapLatest { query ->
            searchMangaRepository.observeMangaResources(
                SearchMangaResourceQuery(title = query)
            )
        }
        .onStart {
            // emits initial search results prefetched for no query
            // this will avoid the initial result being debounced by 3 seconds
            emit(
                searchMangaRepository.observeMangaResources(searchMangaRepository.latestQuery())
                    .first()
            )
        }

    private val loadState = searchMangaRepository.loadState.stateInUi(LoadState.None)

    val searchMangaUiState = combine(
        mangaSearchFlow,
        loadState,
        savedMangaRepository.getSavedMangas(),
    ) { resources, load, saved ->
        val combinedManga = resources.map {
            SavableManga(it, saved.find { s -> s.id ==  it.id})
        }
        when (load) {
            LoadState.End -> {
                SearchMangaUiState.Success.EndOfPagination(
                    results = combinedManga,
                )
            }
            LoadState.Loading -> {
                SearchMangaUiState.Success.Loading(
                    results = combinedManga,
                )
            }
            LoadState.None -> SearchMangaUiState.Success.Idle(
                results = combinedManga,
            )
            LoadState.Refreshing -> SearchMangaUiState.Refreshing
        }
    }
        .stateInUi(SearchMangaUiState.WaitingForQuery)

    val popularMangaUiState = combine(
        popularMangaRepository.observeAllMangaResources(),
        savedMangaRepository.getSavedMangas()
    ) { resources, saved ->
        resources.map {
            SavableManga(it, saved.find { manga -> manga.id == it.id })
        }
    }
        .stateInUi(emptyList())

    val recentMangaUiState = combine(
        recentMangaRepository.observeAllMangaResources(),
        savedMangaRepository.getSavedMangas()
    ) { resources, saved ->
        resources.map {
            SavableManga(it, saved.find { manga -> manga.id == it.id })
        }
    }
        .stateInUi(emptyList())

    val seasonalMangaUiState = combine(
        seasonalMangaRepository.getSeasonalLists(),
        savedMangaRepository.getSavedMangas()
    ) { seasonWithManga, saved ->
        val yearLists = seasonWithManga.map {
            SeasonalList(
                id = it.list.id,
                year = it.list.year,
                season = it.list.season,
                mangas = it.manga.map { m -> SavableManga(m, saved.find { s -> s.id == m.id }) }
            )
        }
            .sortedBy { it.year * 10000 + it.season.ordinal }
            .takeIf { it.size >= 4 }
            ?.takeLast(4)
            ?: return@combine   SeasonalMangaUiState(
                emptyList()
            )
        SeasonalMangaUiState(
            seasonalLists = yearLists
        )
    }
        .stateInUi(SeasonalMangaUiState(emptyList()))

    fun updateSearchQuery(query: String) {
        mutableSearchQuery.update { query }
    }

    fun bookmarkManga(mangaId: String) = coroutineScope.launch {
        savedMangaRepository.bookmarkManga(mangaId)
    }

    fun loadNextPopularPage() = coroutineScope.launch {
        popularMangaRepository.loadNextPage()
    }

    fun loadNextSearchPage() = coroutineScope.launch {
        searchMangaRepository.loadNextPage()
    }

    fun loadNextRecentPage() = coroutineScope.launch {
        recentMangaRepository.loadNextPage()
    }
}


data class SeasonalMangaUiState(
    val seasonalLists: List<SeasonalList> = emptyList()
)

data class SeasonalList(
    val id: String,
    val year: Int,
    val season: Season,
    val mangas: List<SavableManga>
)
