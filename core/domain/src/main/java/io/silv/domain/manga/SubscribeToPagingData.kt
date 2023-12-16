package io.silv.domain.manga

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import io.silv.common.model.PagedType
import io.silv.data.manga.MangaPagingSourceFactory
import io.silv.data.manga.SavedMangaRepository
import io.silv.model.SavableManga
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


class SubscribeToPagingData(
    private val pagingFactory: MangaPagingSourceFactory,
    private val savedMangaRepository: SavedMangaRepository,
) {

    operator fun invoke(
        config: PagingConfig,
        typeFlow: Flow<PagedType>,
        scope: CoroutineScope
    ): StateFlow<Flow<PagingData<SavableManga>>> {
        return typeFlow.distinctUntilChanged()
            .map { type ->
                combine(
                    pagingFactory.pager(config, type).flow.cachedIn(scope),
                    savedMangaRepository.observeSavedMangaList()
                ) { pagingData, saved ->
                        pagingData.map { manga ->
                           saved.find { it.id == manga.id }
                               ?.let(::SavableManga)
                               ?: SavableManga(manga)
                        }
                    }
                    .cachedIn(scope)

            }
            .stateIn(scope, SharingStarted.Lazily, emptyFlow())
    }
}