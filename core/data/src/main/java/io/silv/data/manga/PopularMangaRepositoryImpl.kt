package io.silv.data.manga

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.time.timeStringMinus
import io.silv.data.mappers.toPopularMangaResource
import io.silv.database.AmadeusDatabase
import io.silv.database.dao.PopularMangaResourceDao
import io.silv.database.entity.manga.resource.PopularMangaResource
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import java.time.Duration

@OptIn(ExperimentalPagingApi::class)
private class PopularMangaRemoteMediator(
    private val mangaDexApi: MangaDexApi,
    private val db: AmadeusDatabase,
): RemoteMediator<Int, PopularMangaResource>() {

    private val dao = db.popularMangaResourceDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PopularMangaResource>
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
                    order = mapOf("followedCount" to "desc"),
                    availableTranslatedLanguage = listOf("en"),
                    hasAvailableChapters = true,
                    createdAtSince = timeStringMinus(Duration.ofDays(30))
                )
            )
                .getOrThrow()

            db.withTransaction {
                if(loadType == LoadType.REFRESH) {
                    dao.deleteAll()
                }
                val entities = response.data.map {manga ->
                    manga.toPopularMangaResource().copy(offset = offset)
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


internal class PopularMangaRepositoryImpl(
    private val mangaResourceDao: PopularMangaResourceDao,
    private val popularMangaResourceDao: PopularMangaResourceDao,
    private val dispatchers: io.silv.common.AmadeusDispatchers,
    mangaDexApi: MangaDexApi,
    amadeusDatabase: AmadeusDatabase,
): PopularMangaRepository {

    @OptIn(ExperimentalPagingApi::class)
    override val pager = Pager(
        config = PagingConfig(
            pageSize = 60,
            initialLoadSize = 60
        ),
        remoteMediator = PopularMangaRemoteMediator(mangaDexApi, amadeusDatabase),
        pagingSourceFactory = {
            popularMangaResourceDao.pagingSource()
        }
    )

    override fun observeMangaResourceById(id: String): Flow<PopularMangaResource?> {
        return mangaResourceDao.observePopularMangaResourceById(id).flowOn(dispatchers.io)
    }

    override fun observeAllMangaResources(): Flow<List<PopularMangaResource>> {
        return mangaResourceDao.getPopularMangaResources().flowOn(dispatchers.io)
    }
}