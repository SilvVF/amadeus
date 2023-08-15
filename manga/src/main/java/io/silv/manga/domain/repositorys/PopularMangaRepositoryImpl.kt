package io.silv.manga.domain.repositorys

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.MangaToPopularMangaResourceMapper
import io.silv.manga.domain.suspendRunCatching
import io.silv.manga.domain.timeStringMinus
import io.silv.manga.local.AmadeusDatabase
import io.silv.manga.local.dao.PopularMangaResourceDao
import io.silv.manga.local.entity.PopularMangaResource
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.requests.MangaRequest
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
                val entities = response.data.map {
                    MangaToPopularMangaResourceMapper.map(it to null).copy(offset = offset)
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
    private val dispatchers: AmadeusDispatchers,
    mangaDexApi: MangaDexApi,
    amadeusDatabase: AmadeusDatabase,
): PopularMangaRepository {

    @OptIn(ExperimentalPagingApi::class)
    override val pager = Pager(
        config = PagingConfig(pageSize = 60),
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