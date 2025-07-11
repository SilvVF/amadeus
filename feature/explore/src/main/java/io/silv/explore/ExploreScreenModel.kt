package io.silv.explore


import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.paging.PagingConfig
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.DependencyAccessor
import io.silv.common.log.logcat
import io.silv.common.model.CardType
import io.silv.common.model.PagedType
import io.silv.data.manga.SubscribeToPagingData
import io.silv.data.manga.interactor.MangaHandler
import io.silv.data.manga.repository.SeasonalMangaRepository
import io.silv.data.search.RecentSearchHandler
import io.silv.datastore.SettingsStore
import io.silv.di.dataDeps
import io.silv.model.DomainSeasonalList
import io.silv.model.RecentSearch
import io.silv.sync.SyncManager
import io.silv.sync.syncDependencies
import io.silv.ui.EventStateScreenModel
import io.silv.ui.ioCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.datastore.preferences.core.Preferences
import app.cash.molecule.AndroidUiDispatcher
import io.silv.datastore.Keys

sealed interface ExploreSettingsEvent {
    data class ChangeCardType(val cardType: CardType): ExploreSettingsEvent
    data class ChangeGridCells(val gridCells: Int): ExploreSettingsEvent
    data object ToggleUseList: ExploreSettingsEvent
}

data class ExploreSettings(
    val cardType: CardType = CardType.Compact,
    val gridCells: Int = 2,
    val useList: Boolean = false,
    val events: (ExploreSettingsEvent) -> Unit = {}
)

class ExploreSettingsPresenter(
    parentScope: CoroutineScope,
    private val settingsStore: SettingsStore
) {

    private val scope = CoroutineScope(parentScope.coroutineContext + AndroidUiDispatcher.Main)

    val state = scope.launchMolecule(mode = RecompositionMode.ContextClock) {
        present()
    }

    @Composable
    fun present(): ExploreSettings {
        val scope = rememberCoroutineScope()

        val cardType by settingsStore.exploreCardType.collectAsState()
        val cells by settingsStore.exploreGridCells.collectAsState()
        val useList by rememberUpdatedState(settingsStore.exploreUseList.collectAsState().value)

        fun <T> editSettings(
            key: Preferences.Key<T>,
            value: T
        ) = scope.launch {
            settingsStore.edit { prefs ->
                prefs[key] = value
            }
        }

        return ExploreSettings(
            cardType = cardType,
            gridCells = cells,
            useList = useList
        ) {
            when(it) {
                is ExploreSettingsEvent.ChangeCardType -> editSettings(Keys.ExplorePrefs.cardTypePrefKey, it.cardType.ordinal)
                is ExploreSettingsEvent.ChangeGridCells -> editSettings(Keys.ExplorePrefs.gridCellsPrefKey, it.gridCells)
                ExploreSettingsEvent.ToggleUseList -> editSettings(Keys.ExplorePrefs.useListPrefKey, !useList)
            }
        }
    }
}

class ExploreScreenModel @OptIn(DependencyAccessor::class) constructor(
    savedStatePagedType: UiPagedType?,
    subscribeToPagingData: SubscribeToPagingData = dataDeps.subscribeToPagingData,
    seasonalManga: SeasonalMangaRepository = dataDeps.seasonalMangaRepository,
    private val mangaHandler: MangaHandler = dataDeps.mangaHandler,
    private val recentSearchHandler: RecentSearchHandler = dataDeps.recentSearchHandler,
    private val seasonalMangaSyncManager: SyncManager = syncDependencies.seasonalMangaSyncManager,
    private val settingsStore: SettingsStore = dataDeps.settingsStore
) : EventStateScreenModel<ExploreEvent, ExploreState>(
    ExploreState(
        pagedType = savedStatePagedType ?: UiPagedType.Popular
    )
) {
    val settings = ExploreSettingsPresenter(screenModelScope, settingsStore)

    init {
        settings.state.onEach {
            mutableState.update { state ->
                state.copy(
                    settings = it
                )
            }
        }
            .launchIn(screenModelScope)

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
    val settings: ExploreSettings = ExploreSettings()
) {
    val filters
        get() = (this.pagedType as? UiPagedType.Query)?.filters
}
