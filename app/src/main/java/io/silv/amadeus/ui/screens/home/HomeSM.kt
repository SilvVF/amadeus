package io.silv.amadeus.ui.screens.home

import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.screens.manga_reader.combineToPair
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.DomainTag
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.PopularMangaRepository
import io.silv.manga.domain.repositorys.QuickSearchMangaRepository
import io.silv.manga.domain.repositorys.RecentMangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.SeasonalMangaRepository
import io.silv.manga.domain.repositorys.base.LoadState
import io.silv.manga.domain.repositorys.base.PagedLoadState
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
    private val searchMangaRepository: QuickSearchMangaRepository,
    tagRepository: TagRepository
): AmadeusScreenModel<HomeEvent>() {

    private val mutableSearchQuery = MutableStateFlow(searchMangaRepository.latestQuery())
    val searchQuery = mutableSearchQuery.asStateFlow()


    val refreshingSeasonal = seasonalMangaRepository.loadState
        .map { it is LoadState.Refreshing }
        .stateInUi(false)


    val tagsUiState = tagRepository.allTags().map {
        it.map { tag ->
            DomainTag(tag)
        }
    }
        .stateInUi(emptyList())

    private var startFlag: Boolean = false

    private val startFlow = MutableStateFlow(false)

    init {
        coroutineScope.launch {
            popularMangaRepository.refresh(null)
            recentMangaRepository.refresh(null)
        }
    }

    fun startSearch() {
        startFlag = true
        startFlow.update { !it}
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val mangaSearchFlow = searchQuery
        .combineToPair(startFlow)
        .debounce { if (startFlag.also { startFlag = false }) 0L else 1000L }
        .flatMapLatest { (query, _) ->
            searchMangaRepository.observeMangaResources(query)
        }
        .onStart {
            emit(
                searchMangaRepository.observeMangaResources(searchMangaRepository.latestQuery())
                    .first()
            )
        }

    private val loadState = searchMangaRepository.loadState.stateInUi(PagedLoadState.None)

    val searchMangaUiState = combine(
        mangaSearchFlow,
        savedMangaRepository.getSavedMangas(),
        loadState,
    ) { resource, saved, loadState ->
        val resources = resource.map {
            SavableManga(it, saved.find { manga -> manga.id == it.id })
        }
        when (loadState) {
            PagedLoadState.End -> PaginatedListState.Success(resources, end = true, loading = false)
            is PagedLoadState.Error -> PaginatedListState.Error(
                resources,
                loadState.throwable.localizedMessage ?: "unkown error"
            )
            PagedLoadState.Loading -> PaginatedListState.Success(resources, end = false, loading = true)
            PagedLoadState.None -> PaginatedListState.Success(resources, end = false, loading = false)
            PagedLoadState.Refreshing -> PaginatedListState.Refreshing
        }
    }
        .stateInUi(PaginatedListState.Refreshing)

    val popularMangaUiState = combine(
        popularMangaRepository.observeAllMangaResources(),
        savedMangaRepository.getSavedMangas(),
        popularMangaRepository.loadState,
    ) { resource, saved, loadState ->
        val resources = resource.map {
            SavableManga(it, saved.find { manga -> manga.id == it.id })
        }
        when (loadState) {
            PagedLoadState.End -> PaginatedListState.Success(resources, end = true, loading = false)
            is PagedLoadState.Error -> PaginatedListState.Error(
                resources,
                loadState.throwable.localizedMessage ?: "unkown error"
            )
            PagedLoadState.Loading -> PaginatedListState.Success(resources, end = false, loading = true)
            PagedLoadState.None -> PaginatedListState.Success(resources, end = false, loading = false)
            PagedLoadState.Refreshing -> PaginatedListState.Refreshing
        }
    }
        .stateInUi(PaginatedListState.Refreshing)

    val recentMangaUiState = combine(
        recentMangaRepository.observeAllMangaResources(),
        savedMangaRepository.getSavedMangas(),
        recentMangaRepository.loadState
    ) { resource, saved, loadState ->
        val resources = resource.map {
            SavableManga(it, saved.find { manga -> manga.id == it.id })
        }.chunked(2)
        when (loadState) {
            PagedLoadState.End -> PaginatedListState.Success(resources, end = true, loading = false)
            is PagedLoadState.Error -> PaginatedListState.Error(
                resources,
                loadState.throwable.localizedMessage ?: "unkown error"
            )
            PagedLoadState.Loading -> PaginatedListState.Success(resources, end = false, loading = true)
            PagedLoadState.None -> PaginatedListState.Success(resources, end = false, loading = false)
            PagedLoadState.Refreshing -> PaginatedListState.Refreshing
        }
    }
        .stateInUi(PaginatedListState.Refreshing)

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
            .sortedByDescending { it.year * 10000 + it.season.ordinal }
            .takeIf { it.size >= 4 }
            ?.take(8)
            ?: return@combine SeasonalMangaUiState(
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

sealed class PaginatedListState<out T> {
    object Refreshing: PaginatedListState<Nothing>()
    data class Success<T>(val data: T, val end: Boolean = false, val loading: Boolean = false): PaginatedListState<T>()
    data class Error<T>(val data: T, val message: String) : PaginatedListState<T>()
}

data class SeasonalList(
    val id: String,
    val year: Int,
    val season: Season,
    val mangas: List<SavableManga>
)
