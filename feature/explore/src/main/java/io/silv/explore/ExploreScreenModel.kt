package io.silv.explore

import androidx.paging.cachedIn
import androidx.paging.map
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val searchMangaPagingFlow = combineTuple(
        searchMangaRepository.pager("").flow.cachedIn(screenModelScope),
        savedMangaRepository.getSavedMangas()
    ) .map { (pagingData, saved) ->
          pagingData.map { (_, manga) ->
              SavableManga(manga, saved.find { s -> s.id == manga.id })
          }
    }

    val popularMangaPagingFlow = combineTuple(
        popularMangaRepository.pager.flow.cachedIn(screenModelScope),
        savedMangaRepository.getSavedMangas()
    )
        .map { (pagingData, saved) ->
            pagingData.map { (_, manga) ->
                SavableManga(manga, saved.find { s -> s.id == manga.id })
            }
        }
        .cachedIn(screenModelScope)

    val recentMangaPagingFlow =
        combineTuple(
            recentMangaRepository.pager.flow.cachedIn(screenModelScope),
            savedMangaRepository.getSavedMangas()
        )
        .map { (pagingData, saved) ->
            pagingData.map { (_, manga) ->
                SavableManga(manga, saved.find { it.id == manga.id })
            }
        }

    val seasonalMangaUiState = combineTuple(
        seasonalMangaRepository.getSeasonalLists(),
        savedMangaRepository.getSavedMangas()
    ).map { (seasonWithManga, saved)->
        val yearLists = seasonWithManga.map {(list, mangas) ->
            SeasonalList(
                id = list.id,
                year = list.year,
                season = list.season,
                mangas = mangas.map { manga->
                    SavableManga(
                        manga,
                        saved.find { s -> s.id == manga.id }
                    )
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


