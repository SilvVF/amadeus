package io.silv.domain

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import io.silv.data.manga.PagedType
import io.silv.data.manga.QueryPagingSourceRepo
import io.silv.data.mappers.toSourceManga
import io.silv.database.dao.SourceMangaDao
import io.silv.model.SavableManga
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class GetQueryPagingData(
    private val queryPagingSourceRepo: QueryPagingSourceRepo,
    private val sourceMangaDao: SourceMangaDao,
) {
    operator fun invoke(
        config: PagingConfig,
        typeFlow: Flow<PagedType>,
        scope: CoroutineScope
    ): StateFlow<Flow<PagingData<StateFlow<SavableManga>>>> {
        return typeFlow.distinctUntilChanged()
            .map { type ->
                queryPagingSourceRepo.queryPager(config, type)
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