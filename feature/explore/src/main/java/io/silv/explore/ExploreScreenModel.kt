package io.silv.explore

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.paging.PagingConfig
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.DependencyAccessor
import io.silv.common.log.logcat
import io.silv.common.model.PagedType
import io.silv.data.download.CoverCache
import io.silv.di.dataDeps
import io.silv.data.manga.SubscribeToPagingData
import io.silv.data.manga.interactor.MangaHandler
import io.silv.data.manga.model.toResource
import io.silv.data.manga.repository.SeasonalMangaRepository
import io.silv.data.search.RecentSearchHandler
import io.silv.model.DomainSeasonalList
import io.silv.model.RecentSearch
import io.silv.sync.SyncManager
import io.silv.sync.syncDependencies
import io.silv.ui.EventStateScreenModel
import io.silv.ui.ioCoroutineScope



import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExploreScreenModel @OptIn(DependencyAccessor::class) constructor(
    savedStatePagedType: UiPagedType?,
    subscribeToPagingData: SubscribeToPagingData = dataDeps.subscribeToPagingData,
    seasonalManga: SeasonalMangaRepository = dataDeps.seasonalMangaRepository,
    private val mangaHandler: MangaHandler = dataDeps.mangaHandler,
    private val recentSearchHandler: RecentSearchHandler = dataDeps.recentSearchHandler,
    private val seasonalMangaSyncManager: SyncManager = syncDependencies.seasonalMangaSyncManager
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
                        seasonalLists = lists.toList(),
                    )
                }
            }
            .launchIn(screenModelScope)

        recentSearchHandler.recentSearchList
            .onEach { recentSearchResults ->
                mutableState.update { state ->
                    state.copy(
                        recentSearchUiState = RecentSearchUiState.Success(recentSearchResults.toList()),
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
        }
    }

    fun refreshSeasonalManga() {
        screenModelScope.launch {
            seasonalMangaSyncManager.requestSync()
        }
    }

    fun clearSearchHistory() {
        screenModelScope.launch {
            logcat { "clearSearchHistory" }
            recentSearchHandler.clearRecentSearches()
        }
    }
}

sealed interface RecentSearchUiState {

    @Stable
    data object Loading : RecentSearchUiState

    @Stable
    data class Success(
        val recentQueries: List<RecentSearch> = emptyList(),
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
    val seasonalLists: List<DomainSeasonalList> = emptyList(),
    val recentSearchUiState: RecentSearchUiState = RecentSearchUiState.Loading,
) {
    val filters
        get() = (this.pagedType as? UiPagedType.Query)?.filters
}
