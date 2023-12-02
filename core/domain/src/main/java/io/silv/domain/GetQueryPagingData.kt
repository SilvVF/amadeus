package io.silv.domain

import android.util.Log
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import io.silv.common.model.PagedType
import io.silv.data.manga.MangaPagingSourceFactory
import io.silv.data.manga.SavedMangaRepository
import io.silv.data.mappers.toSourceManga
import io.silv.database.dao.SourceMangaDao
import io.silv.model.SavableManga
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class GetQueryPagingData(
    private val queryPagingSourceRepo: MangaPagingSourceFactory,
    private val sourceMangaDao: SourceMangaDao,
) {
    operator fun invoke(
        config: PagingConfig,
        typeFlow: Flow<PagedType>,
        scope: CoroutineScope
    ): StateFlow<Flow<PagingData<StateFlow<SavableManga>>>> {
        return typeFlow.distinctUntilChanged()
            .map { type ->
                queryPagingSourceRepo.memoryQueryPager(config, type)
                    .flow.map { pagingData ->
                        pagingData.map { manga ->

                            sourceMangaDao.insert(manga.toSourceManga())

                            sourceMangaDao.observeById(manga.id)
                                .filterNotNull()
                                .map {
                                    SavableManga(it, null)
                                }
                                .stateIn(scope)
                        }
                    }
                    .cachedIn(scope)
            }
            .stateIn(scope, SharingStarted.Lazily, emptyFlow())
    }
}


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
                    pagingFactory.pager(type, config).flow,
                    savedMangaRepository.getSavedMangas()
                ) { pagingData, saved ->
                    Log.d("SubscribeToPagingData", "$type $pagingData")
                        pagingData.map { (_, manga) ->
                           // Log.d("SubscribeToPagingData", "$manga")
                            SavableManga(
                                manga,
                                saved.find { it.id == manga.id }
                            )
                        }
                    }
                    .cachedIn(scope)

            }
            .stateIn(scope, SharingStarted.Lazily, emptyFlow())
    }
}