package io.silv.manga.manga_filter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.paging.PagingConfig
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.model.PagedType
import io.silv.common.model.Resource
import io.silv.common.model.TimePeriod
import io.silv.data.manga.FilteredYearlyMangaRepository
import io.silv.data.manga.SavedMangaRepository
import io.silv.domain.SubscribeToPagingData
import io.silv.model.SavableManga
import io.silv.ui.EventStateScreenModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MangaFilterSM(
    private val filteredYearlyMangaRepository: FilteredYearlyMangaRepository,
    subscribeToPagingData: SubscribeToPagingData,
    private val savedMangaRepository: SavedMangaRepository,
    tagId: String
): EventStateScreenModel<MangaFilterEvent, YearlyFilteredUiState>(YearlyFilteredUiState.Loading) {

    private val currentTagId = MutableStateFlow(tagId)

    var currentTag by mutableStateOf("")
        private set

    private val mutableTimePeriod = MutableStateFlow(TimePeriod.AllTime)
    val timePeriod = mutableTimePeriod.asStateFlow()


    init {
        currentTagId.flatMapLatest { tag ->
            filteredYearlyMangaRepository.getYearlyTopMangaByTagId(tag)
        }.onEach {
            mutableState.value = when (val resource = it) {
                is Resource.Failure -> YearlyFilteredUiState.Loading
                Resource.Loading -> YearlyFilteredUiState.Loading
                is Resource.Success -> YearlyFilteredUiState.Success(
                    resource.result.map { SavableManga(it, null) }.toImmutableList()
                )
            }
        }
            .launchIn(screenModelScope)
    }

    fun updateTagId(id: String, name: String) {
        screenModelScope.launch {
            currentTag = name
            currentTagId.emit(id)
        }
    }


    private val scope = CoroutineScope(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    val timePeriodFilteredPagingFlow = subscribeToPagingData(
        PagingConfig(30, 30),
        currentTagId.combine(timePeriod){ tag, time -> PagedType.TimePeriod(tag, time) },
        scope
    )


    fun changeTimePeriod(timePeriod: TimePeriod) {
        screenModelScope.launch {
            mutableTimePeriod.emit(timePeriod)
        }
    }

    fun bookmarkManga(id: String) {
        screenModelScope.launch {
            savedMangaRepository.addMangaToLibrary(id)
        }
    }

    override fun onDispose() {
        super.onDispose()
        scope.cancel()
    }
}

sealed class YearlyFilteredUiState(open val resources: ImmutableList<SavableManga>) {
    data object Loading: YearlyFilteredUiState(persistentListOf())
    data class Success(
        override val resources: ImmutableList<SavableManga>
    ): YearlyFilteredUiState(resources)
}

