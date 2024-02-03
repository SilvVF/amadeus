package io.silv.explore

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.paging.PagingConfig
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.model.PagedType
import io.silv.data.download.CoverCache
import io.silv.domain.manga.SubscribeToPagingData
import io.silv.domain.manga.interactor.MangaHandler
import io.silv.domain.manga.model.toResource
import io.silv.domain.manga.repository.SeasonalMangaRepository
import io.silv.domain.search.RecentSearchHandler
import io.silv.model.DomainSeasonalList
import io.silv.model.RecentSearch
import io.silv.sync.SyncManager
import io.silv.ui.EventStateScreenModel
import io.silv.ui.ioCoroutineScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExploreScreenModel(
    subscribeToPagingData: SubscribeToPagingData,
    seasonalManga: SeasonalMangaRepository,
    private val mangaHandler: MangaHandler,
    private val coverCache: CoverCache,
    private val recentSearchHandler: RecentSearchHandler,
    private val seasonalMangaSyncManager: SyncManager,
    savedStatePagedType: UiPagedType?
) : EventStateScreenModel<ExploreEvent, ExploreState>(ExploreState(pagedType = savedStatePagedType ?: UiPagedType.Popular)) {
    init {
        seasonalMangaSyncManager.isSyncing
            .onEach { refreshing ->
                mutableState.update { state ->
                    state.copy(
                        refreshingSeasonal = refreshing,
                    )
                }
            }
            .launchIn(screenModelScope)

        seasonalManga.subscribe()
            .onEach { lists ->
                mutableState.update { state ->
                    state.copy(
                        seasonalLists = lists.toImmutableList(),
                    )
                }
            }
            .launchIn(screenModelScope)

        recentSearchHandler.recentSearchList
            .onEach { recentSearchResults ->
                mutableState.update { state ->
                    state.copy(
                        recentSearchUiState = RecentSearchUiState.Success(recentSearchResults.toImmutableList()),
                    )
                }
            }
            .launchIn(screenModelScope)
    }

    val mangaPagingFlow =
        subscribeToPagingData(
            typeFlow =
            state.map { it.pagedType }
                .filterNot { pageType -> pageType is UiPagedType.Seasonal }
                .map { pageType ->
                    when (pageType) {
                        UiPagedType.Latest -> PagedType.Latest
                        UiPagedType.Popular -> PagedType.Popular
                        is UiPagedType.Query -> PagedType.Query(
                            pageType.filters.toQueryFilters()
                        )
                        else -> error("Page type could not be converted")
                    }
                }
                .distinctUntilChanged()
                .onEach { pageType ->
                    if (pageType is PagedType.Query && !pageType.filters.title.isNullOrBlank()) {
                        screenModelScope.launch {
                            recentSearchHandler.onSearchTriggered(
                                pageType.filters.title ?: return@launch
                            )
                        }
                    }
                },
            config =
            PagingConfig(
                pageSize = 30,
                prefetchDistance = 30,
                initialLoadSize = 30,
            ),
            scope = ioCoroutineScope,
        )
            .stateInUi(emptyFlow())

    fun changePagingType(type: UiPagedType) {
        screenModelScope.launch {
            mutableState.update { state ->
                state.copy(pagedType = type)
            }
        }
    }

    fun onSearch(query: String) {
        mutableState.update { state ->
            state.copy(
                pagedType =
                (state.pagedType as? UiPagedType.Query)?.copy(
                    filters = state.pagedType.filters.copy(title = query),
                )
                    ?: UiPagedType.Query(UiQueryFilters(title = query)),
            )
        }
    }

    fun bookmarkManga(mangaId: String) {
        screenModelScope.launch {
            mangaHandler.addOrRemoveFromLibrary(mangaId)
                .onSuccess {
                    if (!it.inLibrary) {
                        ioCoroutineScope.launch {
                            coverCache.deleteFromCache(it.toResource(), true)
                        }
                    }
                }
        }
    }

    fun refreshSeasonalManga() {
        screenModelScope.launch {
            seasonalMangaSyncManager.requestSync()
        }
    }

    fun clearSearchHistory() {
        screenModelScope.launch {
            Log.d("search-history", "clearSearchHistory")
            recentSearchHandler.clearRecentSearches()
        }
    }
}

sealed interface RecentSearchUiState {

    @Stable
    data object Loading : RecentSearchUiState

    @Stable
    data class Success(
        val recentQueries: ImmutableList<RecentSearch> = persistentListOf(),
    ) : RecentSearchUiState
}

@Stable
sealed interface UiPagedType {
    data object Popular : UiPagedType

    data object Latest : UiPagedType

    data object Seasonal : UiPagedType

    data class Query(val filters: UiQueryFilters) : UiPagedType
}

@Immutable
@Stable
data class ExploreState(
    val refreshingSeasonal: Boolean = false,
    val pagedType: UiPagedType = UiPagedType.Popular,
    val seasonalLists: ImmutableList<DomainSeasonalList> = persistentListOf(),
    val recentSearchUiState: RecentSearchUiState = RecentSearchUiState.Loading,
) {
    val filters
        get() = (this.pagedType as? UiPagedType.Query)?.filters
}
