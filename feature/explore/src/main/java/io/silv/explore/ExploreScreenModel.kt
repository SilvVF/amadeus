package io.silv.explore

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.paging.PagingConfig
import cafe.adriel.voyager.core.model.screenModelScope
import com.zhuinden.flowcombinetuplekt.combineTuple
import io.silv.common.model.PagedType
import io.silv.common.model.QueryFilters
import io.silv.common.model.Season
import io.silv.data.manga.SavedMangaRepository
import io.silv.data.manga.SeasonalMangaRepository
import io.silv.domain.SubscribeToPagingData
import io.silv.domain.SubscribeToSeasonalLists
import io.silv.model.DomainSeasonalList
import io.silv.model.SavableManga
import io.silv.sync.SyncManager
import io.silv.ui.EventStateScreenModel
import io.silv.ui.ioCoroutineScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExploreScreenModel(
    seasonalMangaRepository: SeasonalMangaRepository,
    private val subscribeToPagingData: SubscribeToPagingData,
    private val savedMangaRepository: SavedMangaRepository,
    private val subscribeToSeasonalLists: SubscribeToSeasonalLists,
    private val seasonalMangaSyncManager: SyncManager,
): EventStateScreenModel<ExploreEvent, ExploreState>(ExploreState()) {

    init {
        seasonalMangaSyncManager.isSyncing.onEach { refreshing ->
            mutableState.update { state ->
                state.copy(
                    refreshingSeasonal = refreshing
                )
            }
        }
            .launchIn(screenModelScope)
    }

    fun startSearching() {
        mutableState.update { state ->
            state.copy(
                forceSearch = true
            )
        }
    }

    val seasonLists = subscribeToSeasonalLists
        .getLists(ioCoroutineScope)
        .map { lists ->
            lists.map(::toUi).toImmutableList()
        }
        .stateInUi(persistentListOf())


    @OptIn(FlowPreview::class)
    val searchFlow = state.map { it.forceSearch to it.searchQuery }
        .debounce { (skip, _) ->
            if (skip) 0L else 1000L
        }
        .map { (_, searchQuery) -> searchQuery }
        .distinctUntilChanged()
        .onEach {
            mutableState.update { state ->
                state.copy(
                    forceSearch = false
                )
            }
        }
        .stateInUi(state.value.searchQuery)

    val mangaPagingFlow = subscribeToPagingData(
        typeFlow =  state.map { it.pagedType }.combine(searchFlow) { pageType, query ->
                when(pageType) {
                    PagedType.Latest -> pageType
                    PagedType.Popular -> pageType
                    is PagedType.Query -> pageType.copy(
                        filters = QueryFilters(query)
                    )
                }
        },
        config = PagingConfig(
            pageSize = 30,
            prefetchDistance = 30,
            initialLoadSize = 30,
        ),
        scope = ioCoroutineScope
    )


    val seasonalMangaUiState = combineTuple(
        seasonalMangaRepository.getSeasonalLists(),
        savedMangaRepository.getSavedMangas()
    ).map { (seasonWithManga, saved)->
        SeasonalMangaUiState(emptyList())
    }
        .stateInUi(SeasonalMangaUiState(emptyList()))



    fun changePagingType(type: PagedType) {
        screenModelScope.launch {
            mutableState.update {state ->
                state.copy(
                    pagedType = type
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        mutableState.update {state ->
            state.copy(
                searchQuery = query
            )
        }
    }

    fun bookmarkManga(mangaId: String) {
        screenModelScope.launch {
            savedMangaRepository.bookmarkManga(mangaId)
        }
    }

    fun refreshSeasonalManga(){
        screenModelScope.launch {
            seasonalMangaSyncManager.requestSync()
        }
    }
}

fun toUi(list: DomainSeasonalList): UiSeasonalList {
    return UiSeasonalList(list.id, list.season, list.year, list.mangas)
}

@Immutable
@Stable
data class UiSeasonalList(
    val id: String,
    val season: Season,
    val year: Int,
    val mangas: ImmutableList<StateFlow<SavableManga>>
)

@Immutable
@Stable
data class ExploreState(
    val searchQuery: String = "",
    val forceSearch: Boolean = false,
    val refreshingSeasonal: Boolean = false,
    val pagedType: PagedType = PagedType.Popular
)