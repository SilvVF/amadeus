package io.silv.explore

import androidx.paging.PagingConfig
import cafe.adriel.voyager.core.model.screenModelScope
import com.zhuinden.flowcombinetuplekt.combineTuple
import io.silv.common.model.PagedType
import io.silv.common.model.QueryFilters
import io.silv.data.manga.SavedMangaRepository
import io.silv.data.manga.SeasonalMangaRepository
import io.silv.domain.SubscribeToPagingData
import io.silv.sync.SyncManager
import io.silv.ui.EventScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExploreScreenModel(
    seasonalMangaRepository: SeasonalMangaRepository,
    private val subscribeToPagingData: SubscribeToPagingData,
    private val savedMangaRepository: SavedMangaRepository,
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

    val searchMangaPagingFlow = subscribeToPagingData(
        typeFlow = flowOf(PagedType.Query(QueryFilters(""))),
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


