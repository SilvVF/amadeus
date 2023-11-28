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
import io.silv.data.mappers.toRecentMangaResource
import io.silv.database.AmadeusDatabase
import io.silv.database.dao.RecentMangaResourceDao
import io.silv.database.entity.manga.resource.RecentMangaResource
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

@OptIn(ExperimentalPagingApi::class)
private class RecentMangaRemoteMediator(
    private val db: AmadeusDatabase,
    private val mangaDexApi: MangaDexApi,
): RemoteMediator<Int, RecentMangaResource>() {

    private val dao = db.recentMangaResourceDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, RecentMangaResource>
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
                    includes = listOf("cover_art", "author", "artist"),
                    availableTranslatedLanguage = listOf("en"),
                    order = mapOf("createdAt" to "desc")
                )
            )
                .getOrThrow()

            db.withTransaction {
                if(loadType == LoadType.REFRESH) {
                    dao.deleteAll()
                }
                val entities = response.data.map { manga ->
                    manga.toRecentMangaResource().copy(offset = offset)
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


internal class RecentMangaRepositoryImpl(
    private val mangaResourceDao: RecentMangaResourceDao,
    private val dispatchers: AmadeusDispatchers,
    mangaDexApi: MangaDexApi,
    amadeusDatabase: AmadeusDatabase,
): RecentMangaRepository {
    @OptIn(ExperimentalPagingApi::class)
    override val pager = Pager(
        config = PagingConfig(
            pageSize = 60,
            initialLoadSize = 60
        ),
        remoteMediator = RecentMangaRemoteMediator(amadeusDatabase, mangaDexApi),
        pagingSourceFactory = {
            mangaResourceDao.pagingSource()
        }
    )


    override fun observeMangaResourceById(id: String): Flow<RecentMangaResource?> {
        return mangaResourceDao.observeRecentMangaResourceById(id)
            .flowOn(dispatchers.io)
    }

    override fun observeAllMangaResources(): Flow<List<RecentMangaResource>> {
        return mangaResourceDao.getRecentMangaResources()
            .flowOn(dispatchers.io)
    }
}