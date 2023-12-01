package io.silv.manga.manga_filter

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.paging.PagingConfig
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.model.LoadState
import io.silv.common.model.PagedType
import io.silv.common.model.TimePeriod
import io.silv.data.manga.FilteredYearlyMangaRepository
import io.silv.data.manga.SavedMangaRepository
import io.silv.domain.SubscribeToPagingData
import io.silv.model.SavableManga
import io.silv.ui.EventScreenModel
import io.silv.ui.ioCoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MangaFilterSM(
    filteredYearlyMangaRepository: FilteredYearlyMangaRepository,
    private val subscribeToPagingData: SubscribeToPagingData,
    private val savedMangaRepository: SavedMangaRepository,
    tagId: String
): EventScreenModel<MangaFilterEvent>() {

    private val yearlyLoadState = filteredYearlyMangaRepository.loadState.stateInUi(LoadState.None)

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
            LoadState.Loading, LoadState.Refreshing -> YearlyFilteredUiState.Loading
            else -> {
                YearlyFilteredUiState.Success(
                    resources.map { r ->
                        SavableManga(r, saved.find { it.id == r.id })
                    }
                )
            }
        }

    }
        .catch { it.printStackTrace() }
        .stateInUi(YearlyFilteredUiState.Loading)


    private val mutableTimePeriod = MutableStateFlow(TimePeriod.AllTime)
    val timePeriod = mutableTimePeriod.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val timePeriodFilteredPagingFlow = subscribeToPagingData(
        config = PagingConfig(30),
        typeFlow = mutableTimePeriod.combine(currentTagId) { tagId, timePeriod ->
            PagedType.Period(timePeriod, tagId)
        },
        scope = ioCoroutineScope
    )


    fun changeTimePeriod(timePeriod: TimePeriod) {
        screenModelScope.launch {
            mutableTimePeriod.emit(timePeriod)
        }
    }

    fun bookmarkManga(id: String) {
        screenModelScope.launch {
            savedMangaRepository.bookmarkManga(id)
        }
    }
}

sealed class YearlyFilteredUiState(open val resources: List<SavableManga>) {
    object Loading: YearlyFilteredUiState(emptyList())
    data class Success(override val resources: List<SavableManga>): YearlyFilteredUiState(resources)
}

