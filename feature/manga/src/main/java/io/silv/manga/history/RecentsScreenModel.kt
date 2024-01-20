package io.silv.manga.history

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.AmadeusDispatchers
import io.silv.domain.history.HistoryRepository
import io.silv.domain.history.HistoryWithRelations
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class RecentsScreenModel(
    private val historyRepository: HistoryRepository,
    dispatchers: AmadeusDispatchers
): StateScreenModel<RecentsState>(RecentsState()) {

    var searchQuery by mutableStateOf("")
        private set

    private val searchFlow = snapshotFlow { searchQuery }
        .debounce(300)
        .distinctUntilChanged()
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ""
        )



    init {
        searchFlow.onStart { emit("") }.flatMapLatest { query ->
            historyRepository.getHistory(query)
        }
            .flowOn(dispatchers.io)
            .onEach {
                mutableState.update { state ->
                    state.copy(
                        history = it.toImmutableList()
                    )
                }
            }
            .launchIn(screenModelScope)
    }

    fun searchChanged(query: String) {
        searchQuery = query
    }

    fun clearHistory() {
        screenModelScope.launch {
            runCatching {
                historyRepository.clearHistory()
            }
        }
    }

    fun deleteItemFromHistory(id: Long) {
        screenModelScope.launch {
            runCatching {
                historyRepository.delete(id)
            }
        }
    }

    fun deleteHistoryByMangaId(mangaId: String){
        screenModelScope.launch {
            runCatching {
                historyRepository.deleteAllForManga(mangaId)
            }
        }
    }
}

@Immutable
data class RecentsState(
    val history: ImmutableList<HistoryWithRelations> = persistentListOf(),
) {

    val groupedByEpochDays = history
        .groupBy { it.lastRead.date.toEpochDays() }
        .toList()
        .toImmutableList()
}

@Stable
data class RecentsActions(
    val searchChanged: (query: String) -> Unit = {},
    val clearHistory: () -> Unit = {},
    val deleteHistory: (id: Long) -> Unit = {},
    val deleteHistoryForManga: (mangaId: String) -> Unit = {},
)