package io.silv.data.manga

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import io.silv.common.model.PagedType
import io.silv.data.manga.interactor.GetManga
import io.silv.data.manga.model.Manga
import io.silv.data.manga.model.toUpdate
import io.silv.data.manga.repository.MangaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn


class SubscribeToPagingData(
    private val pagingFactory: MangaPagingSourceFactory,
    private val getManga: GetManga,
) {
    operator fun invoke(
        config: PagingConfig,
        typeFlow: Flow<PagedType>,
        scope: CoroutineScope,
    ): StateFlow<Flow<PagingData<StateFlow<Manga>>>> {
        return typeFlow.distinctUntilChanged()
            .map { type ->
                pagingFactory.pager(config, type).flow.cachedIn(scope)
                    .map { pagingData ->
                        pagingData.map { manga ->
                            getManga.subscribe(manga.id).stateIn(scope)
                        }
                    }
            }
            .stateIn(
                scope,
                SharingStarted.Lazily,
                emptyFlow()
            )
    }
}
