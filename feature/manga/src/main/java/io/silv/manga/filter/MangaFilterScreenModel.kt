package io.silv.manga.filter

import androidx.paging.PagingConfig
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.DependencyAccessor
import io.silv.common.model.PagedType
import io.silv.common.model.TimePeriod
import io.silv.data.download.CoverCache
import io.silv.di.dataDeps
import io.silv.data.manga.SubscribeToPagingData
import io.silv.data.manga.interactor.MangaHandler
import io.silv.data.manga.model.toResource
import io.silv.ui.EventStateScreenModel
import io.silv.ui.ioCoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MangaFilterScreenModel @OptIn(DependencyAccessor::class) constructor(
    private val mangaHandler: MangaHandler = dataDeps.mangaHandler,
    subscribeToPagingData: SubscribeToPagingData = dataDeps.subscribeToPagingData,
    private val coverCache: CoverCache = dataDeps.coverCache,
    tagId: String,
) : EventStateScreenModel<MangaFilterEvent, YearlyFilteredUiState>(YearlyFilteredUiState()) {

    val timePeriodFilteredPagingFlow =
        subscribeToPagingData(
            config = PagingConfig(30, 30),
            typeFlow = state.map { (time) ->
                PagedType.TimePeriod(tagId, time)
            },
            scope = ioCoroutineScope,
        )

    fun changeTimePeriod(timePeriod: TimePeriod) {
        screenModelScope.launch {
            mutableState.update { it.copy(timePeriod = timePeriod) }
        }
    }

    fun toggleFavorite(id: String) {
        screenModelScope.launch {
            mangaHandler.addOrRemoveFromLibrary(id)
                .onSuccess {
                    if (!it.inLibrary) {
                        ioCoroutineScope.launch {
                            coverCache.deleteFromCache(it.toResource(), true)
                        }
                    }
                }
        }
    }
}

data class YearlyFilteredUiState(
    val timePeriod: TimePeriod = TimePeriod.OneYear
)