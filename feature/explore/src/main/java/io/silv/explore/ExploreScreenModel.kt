package io.silv.explore

import androidx.paging.cachedIn
import androidx.paging.map
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.model.screenModelScope
import com.zhuinden.flowcombinetuplekt.combineTuple
import io.silv.data.manga.PopularMangaRepository
import io.silv.data.manga.QuickSearchMangaRepository
import io.silv.data.manga.RecentMangaRepository
import io.silv.data.manga.SavedMangaRepository
import io.silv.data.manga.SeasonalMangaRepository
import io.silv.explore.SeasonalMangaUiState.SeasonalList
import io.silv.model.SavableManga
import io.silv.sync.SyncManager
import io.silv.ui.EventScreenModel
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

class ExploreScreenModel(
    recentMangaRepository: RecentMangaRepository,
    popularMangaRepository: PopularMangaRepository,
    seasonalMangaRepository: SeasonalMangaRepository,
    private val savedMangaRepository: SavedMangaRepository,
    searchMangaRepository: QuickSearchMangaRepository,
    private val seasonalMangaSyncManager: SyncManager,
): EventScreenModel<ExploreEvent>() {

    private val mutableSearchQuery = MutableStateFlow("")
    val searchQuery = mutableSearchQuery.asStateFlow()

    val refreshingSeasonal = seasonalMangaSyncManager.isSyncing.stateInUi(false)

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
            .cachedIn(screenModelScope)

    val searchMangaPagingFlow = combineTuple(
        mangaSearchFlow,
        savedMangaRepository.getSavedMangas(),
    ).map { (pagingData, saved) ->
        pagingData.map {
           SavableManga(it, saved.find { s -> s.id == it.id })
        }
    }

    val popularMangaPagingFlow = combineTuple(
        popularMangaRepository.pager.flow.cachedIn(screenModelScope),
        savedMangaRepository.getSavedMangas()
    )
        .map { (pagingData, saved) ->
            pagingData.map { SavableManga(it, saved.find { s -> s.id == it.id }) }
        }
        .cachedIn(screenModelScope)

    val recentMangaPagingFlow = combineTuple(
        recentMangaRepository.pager.flow.cachedIn(screenModelScope),
        savedMangaRepository.getSavedMangas(),
    ).map { (pagingData, saved) ->
        pagingData.map { SavableManga(it, saved.find { s -> s.id == it.id }) }
    }
        .cachedIn(screenModelScope)

    val seasonalMangaUiState = combineTuple(
        seasonalMangaRepository.getSeasonalLists(),
        savedMangaRepository.getSavedMangas()
    ).map { (seasonWithManga, saved)->
        val yearLists = seasonWithManga.map {
            SeasonalList(
                id = it.list.id,
                year = it.list.year,
                season = it.list.season,
                mangas = it.manga.map { m ->
                    SavableManga(
                        m,
                        saved.find { s -> s.id == m.id })
                }
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


