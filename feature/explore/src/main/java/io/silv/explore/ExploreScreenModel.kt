package io.silv.explore

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.paging.PagingConfig
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.model.PagedType
import io.silv.common.model.Season
import io.silv.data.manga.SavedMangaRepository
import io.silv.domain.RecentSearchHandler
import io.silv.domain.SubscribeToPagingData
import io.silv.domain.SubscribeToSeasonalLists
import io.silv.model.DomainSeasonalList
import io.silv.model.RecentSearch
import io.silv.model.SavableManga
import io.silv.sync.SyncManager
import io.silv.ui.EventStateScreenModel
import io.silv.ui.ioCoroutineScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExploreScreenModel(
    subscribeToPagingData: SubscribeToPagingData,
    subscribeToSeasonalLists: SubscribeToSeasonalLists,
    private val recentSearchHandler: RecentSearchHandler,
    private val savedMangaRepository: SavedMangaRepository,
    private val seasonalMangaSyncManager: SyncManager,
): EventStateScreenModel<ExploreEvent, ExploreState>(ExploreState()) {

    init {
        seasonalMangaSyncManager.isSyncing
            .onEach { refreshing ->
                mutableState.update { state ->
                    state.copy(
                        refreshingSeasonal = refreshing
                    )
                }
            }
            .launchIn(screenModelScope)

        subscribeToSeasonalLists.getLists(ioCoroutineScope)
            .onEach { lists ->
                mutableState.update { state ->
                    state.copy(
                        seasonalLists = lists.map(::toUi)
                            .toImmutableList()
                    )
                }
            }
            .launchIn(screenModelScope)

        recentSearchHandler.recentSearchList
            .onEach { recentSearchResults ->
                mutableState.update { state ->
                    state.copy(
                        recentSearchUiState = RecentSearchUiState.Success(recentSearchResults)
                    )
                }
            }
            .launchIn(screenModelScope)
    }

    val mangaPagingFlow = subscribeToPagingData(
        typeFlow =  state.map { it.pagedType }
            .filterNot { pageType -> pageType is UiPagedType.Seasonal }
            .map { pageType ->
                when(pageType) {
                    UiPagedType.Latest -> PagedType.Latest
                    UiPagedType.Popular -> PagedType.Popular
                    is UiPagedType.Query -> PagedType.Query(pageType.filters.toQueryFilters())
                    else -> error("Page type could not be converted")
                }
            }
            .onEach { pageType ->
                if (pageType is PagedType.Query && !pageType.filters.title.isNullOrBlank()) {
                    screenModelScope.launch {
                        recentSearchHandler.onSearchTriggered(pageType.filters.title ?: return@launch)
                    }
                }
        },
        config = PagingConfig(
            pageSize = 30,
            prefetchDistance = 30,
            initialLoadSize = 30,
        ),
        scope = ioCoroutineScope
    )


    fun changePagingType(type: UiPagedType) {
        screenModelScope.launch {
            mutableState.update {state ->
                state.copy(pagedType = type)
            }
        }
    }

    fun onSearch(query: String) {
        mutableState.update { state ->
            state.copy(
                pagedType = (state.pagedType as? UiPagedType.Query)?.copy(
                    filters = state.pagedType.filters.copy(title = query)
                )
                    ?: UiPagedType.Query(UiQueryFilters(title = query))
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

sealed interface RecentSearchUiState {
    data object Loading : RecentSearchUiState

    data class Success(
        val recentQueries: ImmutableList<RecentSearch> = persistentListOf(),
    ) : RecentSearchUiState
}

@Stable
sealed interface UiPagedType {
    data object Popular: UiPagedType
    data object Latest: UiPagedType
    data object Seasonal: UiPagedType
    data class Query(val filters: UiQueryFilters): UiPagedType
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
    val forceSearch: Boolean = false,
    val refreshingSeasonal: Boolean = false,
    val pagedType: UiPagedType = UiPagedType.Popular,
    val seasonalLists: ImmutableList<UiSeasonalList> = persistentListOf(),
    val recentSearchUiState: RecentSearchUiState = RecentSearchUiState.Loading
)