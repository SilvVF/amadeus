package io.silv.explore

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.paging.PagingConfig
import cafe.adriel.voyager.core.model.screenModelScope
import com.zhuinden.flowcombinetuplekt.combineTuple
import io.silv.common.model.PagedType
import io.silv.common.model.QueryFilters
import io.silv.data.manga.SavedMangaRepository
import io.silv.data.manga.SeasonalMangaRepository
import io.silv.domain.SubscribeToPagingData
import io.silv.sync.SyncManager
import io.silv.ui.EventStateScreenModel
import io.silv.ui.ioCoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExploreScreenModel(
    seasonalMangaRepository: SeasonalMangaRepository,
    private val subscribeToPagingData: SubscribeToPagingData,
    private val savedMangaRepository: SavedMangaRepository,
    private val seasonalMangaSyncManager: SyncManager,
): EventStateScreenModel<ExploreEvent, ExploreState>(ExploreState()) {

    init {
        screenModelScope.launch {
            seasonalMangaSyncManager.isSyncing.collect { refreshing ->
                mutableState.update { state ->
                    state.copy(
                        refreshingSeasonal = refreshing
                    )
                }
            }
        }
    }

    fun startSearching() {
        mutableState.update { state ->
            state.copy(
                forceSearch = true
            )
        }
    }

    @OptIn(FlowPreview::class)
    val searchFlow = state.map { it.forceSearch to it.searchQuery }
        .debounce { (skip, _) ->
            if (skip) 0L else 1000L
        }
        .map { (_, searchQuery) -> searchQuery }
        .distinctUntilChanged()
        .onEach {
            mutableState.update {state ->
                state.copy(
                    forceSearch = false
                )
            }
        }
        .stateInUi(state.value.searchQuery)

    val searchMangaPagingFlow = subscribeToPagingData(
        typeFlow =  searchFlow.map { query -> PagedType.Query(QueryFilters(query = query)) },
        config = PagingConfig(30),
        scope = ioCoroutineScope
    )

    val popularMangaPagingFlow = subscribeToPagingData(
        typeFlow = flowOf(PagedType.Popular),
        config = PagingConfig(30),
        scope = ioCoroutineScope
    )

    val recentMangaPagingFlow = subscribeToPagingData(
        typeFlow = flowOf(PagedType.Latest),
        config = PagingConfig(30),
        scope = ioCoroutineScope
    )

    val seasonalMangaUiState = combineTuple(
        seasonalMangaRepository.getSeasonalLists(),
        savedMangaRepository.getSavedMangas()
    ).map { (seasonWithManga, saved)->
        SeasonalMangaUiState(emptyList())
    }
        .stateInUi(SeasonalMangaUiState(emptyList()))



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

@Immutable
@Stable
data class ExploreState(
    val searchQuery: String = "",
    val forceSearch: Boolean = false,
    val refreshingSeasonal: Boolean = false,
)