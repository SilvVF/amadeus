package io.silv.amadeus.ui.screens.manga_filter

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.paging.cachedIn
import androidx.paging.map
import cafe.adriel.voyager.core.model.coroutineScope
import com.zhuinden.flowcombinetuplekt.combineTuple
import io.silv.amadeus.types.SavableManga
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.common.model.TimePeriod
import io.silv.data.manga.FilteredYearlyMangaRepository
import io.silv.data.manga.SavedMangaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MangaFilterSM(
    private val filteredMangaRepository: io.silv.data.manga.FilteredMangaRepository,
    filteredYearlyMangaRepository: FilteredYearlyMangaRepository,
    private val savedMangaRepository: SavedMangaRepository,
    tagId: String
): AmadeusScreenModel<MangaFilterEvent>() {

    private val yearlyLoadState = filteredYearlyMangaRepository.loadState.stateInUi(io.silv.common.model.LoadState.None)

    private val currentTagId = MutableStateFlow(tagId)
    var currentTag by mutableStateOf("")
        private set

    fun updateTagId(id: String, name: String) {
        currentTag = name
        currentTagId.update { id }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val yearlyManga = currentTagId.flatMapLatest {
        filteredYearlyMangaRepository.getYearlyTopResources(it).onEach { Log.d("YEARLY", it.size.toString()) }
    }

    val yearlyFilteredUiState = combine(
        yearlyLoadState,
        yearlyManga,
        savedMangaRepository.getSavedMangas()
    ) { loadState, resources, saved ->
        when (loadState) {
            io.silv.common.model.LoadState.Loading, io.silv.common.model.LoadState.Refreshing -> YearlyFilteredUiState.Loading
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


    private val mutableTimePeriod = MutableStateFlow(TimePeriod.AllTime)
    val timePeriod = mutableTimePeriod.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val timePeriodFilteredResources = combineTuple(mutableTimePeriod, currentTagId)
        .flatMapLatest { (time, tag) ->
            filteredMangaRepository
                .pager(io.silv.data.manga.FilteredResourceQuery(tag, time))
                .flow
                .cachedIn(coroutineScope)
    }
            .cachedIn(coroutineScope)

    val timePeriodFilteredPagingFlow = combineTuple(
        timePeriodFilteredResources,
        savedMangaRepository.getSavedMangas()
    ).map { (pagingData, saved) ->
        pagingData.map {
            SavableManga(it, saved.find { s -> s.id == it.id })
        }
    }

    fun changeTimePeriod(timePeriod: TimePeriod) = coroutineScope.launch {
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

