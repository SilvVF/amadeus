package io.silv.manga.manga_filter

import androidx.paging.PagingConfig
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.model.PagedType
import io.silv.common.model.TimePeriod
import io.silv.data.manga.SavedMangaRepository
import io.silv.domain.manga.GetSavableManga
import io.silv.domain.manga.SubscribeToPagingData
import io.silv.model.SavableManga
import io.silv.ui.EventStateScreenModel
import io.silv.ui.ioCoroutineScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MangaFilterScreenModel(
    getManga: GetSavableManga,
    subscribeToPagingData: SubscribeToPagingData,
    private val savedMangaRepository: SavedMangaRepository,
    tagId: String,
) : EventStateScreenModel<MangaFilterEvent, YearlyFilteredUiState>(YearlyFilteredUiState.Loading) {
    private val mutableTimePeriod = MutableStateFlow(TimePeriod.AllTime)
    val timePeriod = mutableTimePeriod.asStateFlow()

    init {
        screenModelScope.launch {
            val mangaList = getManga.getYearlyTopMangaByTagId(tagId, ioCoroutineScope)
            mutableState.value = YearlyFilteredUiState.Success(mangaList.toImmutableList())
        }
    }

    val timePeriodFilteredPagingFlow =
        subscribeToPagingData(
            config = PagingConfig(30, 30),
            typeFlow = timePeriod.map { time -> PagedType.TimePeriod(tagId, time) },
            scope = ioCoroutineScope,
        )

    fun changeTimePeriod(timePeriod: TimePeriod) {
        screenModelScope.launch {
            mutableTimePeriod.emit(timePeriod)
        }
    }

    fun bookmarkManga(id: String) {
        screenModelScope.launch {
            savedMangaRepository.addOrRemoveFromLibrary(id)
        }
    }
}

sealed interface YearlyFilteredUiState {
    data object Loading : YearlyFilteredUiState

    data class Success(
        val resources: ImmutableList<StateFlow<SavableManga>>,
    ) : YearlyFilteredUiState
}
