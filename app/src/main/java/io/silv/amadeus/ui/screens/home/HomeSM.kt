package io.silv.amadeus.ui.screens.home

import androidx.paging.cachedIn
import androidx.paging.map
import cafe.adriel.voyager.core.model.coroutineScope
import com.zhuinden.flowcombinetuplekt.combineTuple
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.PopularMangaRepository
import io.silv.manga.domain.repositorys.QuickSearchMangaRepository
import io.silv.manga.domain.repositorys.RecentMangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.SeasonalMangaRepository
import io.silv.manga.domain.repositorys.base.LoadState
import io.silv.manga.local.entity.Season
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeSM(
    recentMangaRepository: RecentMangaRepository,
    popularMangaRepository: PopularMangaRepository,
    seasonalMangaRepository: SeasonalMangaRepository,
    private val savedMangaRepository: SavedMangaRepository,
    searchMangaRepository: QuickSearchMangaRepository,
): AmadeusScreenModel<HomeEvent>() {

    private val mutableSearchQuery = MutableStateFlow("")
    val searchQuery = mutableSearchQuery.asStateFlow()

    val refreshingSeasonal = seasonalMangaRepository.loadState
        .map { it is LoadState.Refreshing }
        .stateInUi(false)

    private val forceSearchFlow = MutableStateFlow(false)
    private var startFlag = false

    fun startSearching() {
        startFlag = true
        forceSearchFlow.update { !it }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val mangaSearchFlow = combineTuple(searchQuery, forceSearchFlow)
        .debounce { if (startFlag.also { startFlag = false }) {  0L } else 2000L }
        .distinctUntilChanged()
        .flatMapLatest { (query, _) ->
            searchMangaRepository.pager(query).flow.cachedIn(coroutineScope)
        }
            .cachedIn(coroutineScope)

    val searchMangaPagingFlow = combineTuple(
        mangaSearchFlow,
        savedMangaRepository.getSavedMangas(),
    ).map { (pagingData, saved) ->
        pagingData.map {
            SavableManga(it, saved.find { s -> s.id == it.id })
        }
    }

    val popularMangaPagingFlow = combineTuple(
        popularMangaRepository.pager.flow.cachedIn(coroutineScope),
        savedMangaRepository.getSavedMangas()
    )
        .map { (pagingData, saved) ->
            pagingData.map { SavableManga(it, saved.find { s -> s.id == it.id }) }
        }

    val recentMangaPagingFlow = combineTuple(
        recentMangaRepository.pager.flow.cachedIn(coroutineScope),
        savedMangaRepository.getSavedMangas(),
    ).map { (pagingData, saved) ->
        pagingData.map { SavableManga(it, saved.find { s -> s.id == it.id }) }
    }



    val seasonalMangaUiState = combineTuple(
        seasonalMangaRepository.getSeasonalLists(),
        savedMangaRepository.getSavedMangas()
    ).map { (seasonWithManga, saved)->
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
            ?: return@map SeasonalMangaUiState(
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
}


data class SeasonalMangaUiState(
    val seasonalLists: List<SeasonalList> = emptyList()
)

sealed class PaginatedListState<out T>() {
    object Refreshing: PaginatedListState<Nothing>()
    data class Success<T>(val data: T, val end: Boolean = false, val loading: Boolean = false): PaginatedListState<T>()
    data class Error<T>(val data: T, val message: String) : PaginatedListState<T>()

    val getData: T?
        get() = when(this) {
            is Error -> this.data
            Refreshing -> null
            is Success -> this.data
        }

    val success: Success<out T>?
        get() = this as? Success<T>
}

data class SeasonalList(
    val id: String,
    val year: Int,
    val season: Season,
    val mangas: List<SavableManga>
)
