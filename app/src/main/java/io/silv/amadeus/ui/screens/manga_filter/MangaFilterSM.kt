package io.silv.amadeus.ui.screens.manga_filter

import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.FilteredMangaRepository
import io.silv.manga.domain.repositorys.FilteredResourceQuery
import io.silv.manga.domain.repositorys.FilteredYearlyMangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.base.LoadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class MangaFilterSM(
    private val filteredMangaRepository: FilteredMangaRepository,
    filteredYearlyMangaRepository: FilteredYearlyMangaRepository,
    private val savedMangaRepository: SavedMangaRepository,
    tagId: String
): AmadeusScreenModel<MangaFilterEvent>() {

    private val yearlyLoadState = filteredYearlyMangaRepository.loadState.stateInUi(LoadState.None)
    private val timeLoadState = filteredMangaRepository.loadState.stateInUi(LoadState.None)

    val yearlyFilteredUiState = combine(
        yearlyLoadState,
        filteredYearlyMangaRepository.getYearlyTopResources(tagId),
        savedMangaRepository.getSavedMangas()
    ) { loadState, resources, saved ->
        when (loadState) {
            LoadState.Refreshing, LoadState.Loading -> YearlyFilteredUiState.Loading
            else -> {
                YearlyFilteredUiState.Success(
                    resources.map { r ->
                        SavableManga(r, saved.find { it.id == r.id })
                    }
                )
            }
        }
    }
        .stateInUi(YearlyFilteredUiState.Loading)


    private val mutableTimePeriod = MutableStateFlow(FilteredMangaRepository.TimePeriod.AllTime)
    val timePeriod = mutableTimePeriod.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val timePeriodFilteredResources = mutableTimePeriod.flatMapLatest {
        filteredMangaRepository.observeMangaResources(FilteredResourceQuery(tagId, it))
    }

    val timePeriodFilteredUiState = combine(
        timeLoadState,
        timePeriodFilteredResources,
        savedMangaRepository.getSavedMangas()
    ) { loadState, resources, saved ->
        when (loadState) {
            LoadState.Refreshing -> TimeFilteredUiState.Loading
            else -> {
                TimeFilteredUiState.Success(
                    resources.map { r ->
                        SavableManga(r, saved.find { it.id == r.id })
                    }
                )
            }
        }
    }
        .stateInUi(TimeFilteredUiState.Loading)


    fun loadNextPage() = coroutineScope.launch {
        filteredMangaRepository.loadNextPage()
    }

    fun changeTimePeriod(timePeriod: FilteredMangaRepository.TimePeriod) = coroutineScope.launch {
        mutableTimePeriod.emit(timePeriod)
    }

    fun bookmarkManga(id: String) = coroutineScope.launch {
        savedMangaRepository.bookmarkManga(id)
    }
}

sealed class YearlyFilteredUiState(open val resources: List<SavableManga>) {
    object Loading: YearlyFilteredUiState(emptyList())
    data class Success(override val resources: List<SavableManga>): YearlyFilteredUiState(resources)
}

sealed class TimeFilteredUiState(open val resources: List<SavableManga>) {
    object Loading: TimeFilteredUiState(emptyList())
    data class Success(override val resources: List<SavableManga>): TimeFilteredUiState(resources)
}

