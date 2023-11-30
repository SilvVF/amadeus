package io.silv.data.manga

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.map
import androidx.room.withTransaction
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.time.timeStringMinus
import io.silv.data.mappers.toSourceManga
import io.silv.database.AmadeusDatabase
import io.silv.database.dao.remotekeys.PopularRemoteKeyWithManga
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.database.entity.manga.remotekeys.PopularRemoteKey
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalPagingApi::class)
private class PopularMangaRemoteMediator(
    private val mangaDexApi: MangaDexApi,
    private val db: AmadeusDatabase,
): RemoteMediator<Int, PopularRemoteKeyWithManga>() {

    private val mangaDao = db.sourceMangaDao()
    private val remoteKeysDao = db.popularRemoteKeysDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PopularRemoteKeyWithManga>
    ): MediatorResult {
        return suspendRunCatching {
            val offset = when(loadType) {
                LoadType.REFRESH -> 0
                LoadType.PREPEND -> return@suspendRunCatching MediatorResult.Success(
                    endOfPaginationReached = true
                )
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                    lastItem?.key?.offset ?: 0
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
                    createdAtSince = timeStringMinus(30.days)
                )
            )
                .getOrThrow()

            db.withTransaction {
                if(loadType == LoadType.REFRESH) {
                    remoteKeysDao.clear()
                }
                val entities = response.data.mapIndexed { i, manga ->
                    manga.toSourceManga().also {
                        remoteKeysDao.insert(
                            PopularRemoteKey(it.id, offset + i)
                        )
                    }
                }
                mangaDao.insertAll(entities)
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
    mangaDexApi: MangaDexApi,
    private val amadeusDatabase: AmadeusDatabase,
): PopularMangaRepository {

    @OptIn(ExperimentalPagingApi::class)
    private val pager = Pager(
        config = PagingConfig(
            pageSize = 60,
            initialLoadSize = 60
        ),
        remoteMediator = PopularMangaRemoteMediator(mangaDexApi, amadeusDatabase),
        pagingSourceFactory = {
            amadeusDatabase.popularRemoteKeysDao().getPagingSource()
        }
    )
    override val pagingData: Flow<PagingData<SourceMangaResource>> = pager.flow.map { pagingData ->
        pagingData.map { (_, manga) ->
            manga
        }
    }
}