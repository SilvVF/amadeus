package io.silv.data.manga

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import io.silv.common.AmadeusDispatchers
import io.silv.common.coroutine.suspendRunCatching
import io.silv.data.mappers.toQuickSearchMangaResource
import io.silv.database.AmadeusDatabase
import io.silv.database.dao.QuickSearchMangaResourceDao
import io.silv.database.entity.manga.resource.QuickSearchMangaResource
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

@OptIn(ExperimentalPagingApi::class)
private class QuickSearchRemoteMediator(
    private val query: String,
    private val db: AmadeusDatabase,
    private val mangaDexApi: MangaDexApi,
): RemoteMediator<Int, QuickSearchMangaResource>() {

    private val dao = db.quickSearchResourceDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, QuickSearchMangaResource>
    ): MediatorResult {
        return suspendRunCatching {
            val offset = when(loadType) {
                LoadType.REFRESH -> 0
                LoadType.PREPEND -> return@suspendRunCatching MediatorResult.Success(
                    endOfPaginationReached = true
                )
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                    if(lastItem == null) { 0 } else {
                        lastItem.offset + state.config.pageSize
                    }
                }
            }
            val response = mangaDexApi.getMangaList(
                MangaRequest(
                    offset = offset,
                    limit = state.config.pageSize,
                    title = query,
                    includes = listOf("cover_art", "author", "artist"),
                    availableTranslatedLanguage = listOf("en"),
                )
            )
                .getOrThrow()

            db.withTransaction {
                if(loadType == LoadType.REFRESH) {
                    dao.deleteAll()
                }
                val entities = response.data.map {manga ->
                    manga.toQuickSearchMangaResource().copy(offset = offset)
                }
                dao.upsertAll(entities)
            }
            MediatorResult.Success(
                endOfPaginationReached = offset + response.data.size >= response.total
            )
        }.getOrElse {
            MediatorResult.Error(it)
        }
    }
}

internal class QuickSearchMangaRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val amadeusDatabase: AmadeusDatabase,
    private val mangaResourceDao: QuickSearchMangaResourceDao,
    private val dispatchers: AmadeusDispatchers,
): QuickSearchMangaRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun pager(query: String) = Pager(
        config = PagingConfig(
            pageSize = 60,
            initialLoadSize = 60
        ),
        remoteMediator = QuickSearchRemoteMediator(query, amadeusDatabase, mangaDexApi),
        pagingSourceFactory = {
            mangaResourceDao.pagingSource()
        }
    )

    override fun observeMangaResourceById(id: String): Flow<QuickSearchMangaResource?> {
        return mangaResourceDao.observeQuickSearchMangaResourceById(id).flowOn(dispatchers.io)
    }

    override fun observeAllMangaResources(): Flow<List<QuickSearchMangaResource>> {
        return mangaResourceDao.observeAllSearchMangaResources().flowOn(dispatchers.io)
    }
}