package io.silv.amadeus.ui.screens.home

import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.repositorys.MangaQuery
import io.silv.manga.domain.repositorys.MangaRepository
import io.silv.manga.sync.SyncManager
import kotlinx.coroutines.launch

class HomeSM(
    private val mangaRepository: MangaRepository,
    private val syncManager: SyncManager,
): AmadeusScreenModel<HomeEvent>() {

    val isSyncing = syncManager.isSyncing
        .stateInUi(false)

    val mangaUiState = mangaRepository.getMagnaResources(
        MangaQuery(emptyList())
    )
        .stateInUi(emptyList())

    fun goToNextPage() {
        coroutineScope.launch {
           syncManager.requestSync()
        }
    }
}

sealed interface HomeEvent
