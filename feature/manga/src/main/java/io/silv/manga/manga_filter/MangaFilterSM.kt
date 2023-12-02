package io.silv.manga.manga_filter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.paging.PagingConfig
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.model.PagedType
import io.silv.common.model.TimePeriod
import io.silv.data.manga.FilteredYearlyMangaRepository
import io.silv.data.manga.SavedMangaRepository
import io.silv.domain.SubscribeToPagingData
import io.silv.model.SavableManga
import io.silv.ui.EventScreenModel
import io.silv.ui.ioCoroutineScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MangaFilterSM(
    filteredYearlyMangaRepository: FilteredYearlyMangaRepository,
    private val subscribeToPagingData: SubscribeToPagingData,
    tagId: String,
    private val savedMangaRepository: SavedMangaRepository
): EventScreenModel<MangaFilterEvent>() {

    private val currentTagId = MutableStateFlow(tagId)

    var currentTag by mutableStateOf("")
        private set

    fun updateTagId(id: String, name: String) {
        currentTag = name
        currentTagId.update { id }
    }


    private val mutableTimePeriod = MutableStateFlow(TimePeriod.AllTime)
    val timePeriod = mutableTimePeriod.asStateFlow()


    val timePeriodFilteredPagingFlow = subscribeToPagingData(
        config = PagingConfig(
            pageSize = 30,
            prefetchDistance = 30,
            initialLoadSize = 30,
        ),
        typeFlow = timePeriod.map { PagedType.Period(tagId, it) },
        scope = ioCoroutineScope
    )



//    @OptIn(ExperimentalCoroutinesApi::class)
//    val yearlyManga = currentTagId.flatMapLatest {
////        filteredYearlyMangaRepository.collectYearlyTopByTagId(it)
//    }
//        .stateInUi(Resource.Loading)

    val yearlyFilteredUiState = flowOf<YearlyFilteredUiState>(YearlyFilteredUiState.Loading)
//    val yearlyFilteredUiState = combine(
//        //yearlyManga,
//        emptyFlow(),
//        savedMangaRepository.getSavedMangas()
//    ) {  resource, saved ->
////        when (resource) {
////            is Resource.Failure -> YearlyFilteredUiState.Success(persistentListOf())
////            Resource.Loading -> YearlyFilteredUiState.Loading
////            is Resource.Success -> YearlyFilteredUiState.Success(
////                resource.result.map { source ->
////                    SavableManga(source, saved.find { source.id == it.id })
////                }
////                    .toImmutableList()
////            )
////        }
//
//    }
//        .catch { it.printStackTrace() }
//        .stateInUi(YearlyFilteredUiState.Loading)


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

sealed class YearlyFilteredUiState(open val resources: ImmutableList<SavableManga>) {
    data object Loading: YearlyFilteredUiState(persistentListOf())
    data class Success(
        override val resources: ImmutableList<SavableManga>
    ): YearlyFilteredUiState(resources)
}

