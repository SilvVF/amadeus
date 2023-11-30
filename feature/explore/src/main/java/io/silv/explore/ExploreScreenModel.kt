package io.silv.explore

import androidx.paging.PagingConfig
import cafe.adriel.voyager.core.model.screenModelScope
import com.zhuinden.flowcombinetuplekt.combineTuple
import io.silv.data.manga.PagedType
import io.silv.data.manga.PopularMangaRepository
import io.silv.data.manga.QuickSearchMangaRepository
import io.silv.data.manga.RecentMangaRepository
import io.silv.data.manga.SavedMangaRepository
import io.silv.data.manga.SeasonalMangaRepository
import io.silv.domain.CombineSourceMangaWithSaved
import io.silv.domain.GetQueryPagingData
import io.silv.sync.SyncManager
import io.silv.ui.EventScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExploreScreenModel(
    recentMangaRepository: RecentMangaRepository,
    popularMangaRepository: PopularMangaRepository,
    seasonalMangaRepository: SeasonalMangaRepository,
    combineSourceMangaWithSaved: CombineSourceMangaWithSaved,
    searchMangaRepository: QuickSearchMangaRepository,
    getQueryPagingData: GetQueryPagingData,
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

    private val pagingType = MutableStateFlow(PagedType.Popular)

    val pagingFlowFlow = getQueryPagingData.invoke(
        config = PagingConfig(
            pageSize = 25
        ),
        pagingType,
        ioCoroutineScope
    )

    val searchMangaPagingFlow = combineSourceMangaWithSaved(
        pagingData = searchMangaRepository.pagingData(""),
        scope = screenModelScope
    )

    val popularMangaPagingFlow = combineSourceMangaWithSaved(
        pagingData = popularMangaRepository.pagingData,
        scope = screenModelScope
    )

    val recentMangaPagingFlow = combineSourceMangaWithSaved(
        pagingData = recentMangaRepository.recentMangaPagingData(
            PagingConfig(
                pageSize = 30,
                initialLoadSize = 30
            )
        ),
        scope = screenModelScope
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


