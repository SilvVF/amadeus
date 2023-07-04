package io.silv.amadeus.ui.screens.home

import androidx.work.Data
import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.repositorys.CombinedResourceSavedMangaRepository
import io.silv.manga.domain.repositorys.MangaQuery
import io.silv.manga.domain.repositorys.MangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.sync.SyncManager
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HomeSM(
    private val combinedResourceSavedMangaRepository: CombinedResourceSavedMangaRepository,
    private val mangaRepository: SavedMangaRepository,
    private val syncManager: SyncManager,
): AmadeusScreenModel<HomeEvent>() {

    val isSyncing = syncManager.isSyncing
        .stateInUi(false)

    val mangaUiState = combinedResourceSavedMangaRepository.observeAll(
        MangaQuery(emptyList())
    )
        .onEach {
            it.filter { it.bookmarked }.forEach {
                println(
                    "${it.id}"
                )
            }
        }
        .stateInUi(emptyList())

    fun bookmarkManga(mangaId: String) = coroutineScope.launch {
        println(mangaId)
        mangaRepository.bookmarkManga(mangaId)
    }

    fun goToNextPage() {
        coroutineScope.launch {
           syncManager.requestSync(Data.EMPTY)
        }
    }
}

sealed interface HomeEvent
