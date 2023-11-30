package io.silv.data.manga

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.cachedIn
import androidx.paging.map
import androidx.room.withTransaction
import io.silv.common.coroutine.suspendRunCatching
import io.silv.data.mappers.toSourceManga
import io.silv.database.AmadeusDatabase
import io.silv.database.dao.remotekeys.RecentRemoteKeyWithSourceManga
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.database.entity.manga.remotekeys.RecentMangaRemoteKey
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalPagingApi::class)
private class RecentMangaRemoteMediator(
    private val db: AmadeusDatabase,
    private val mangaDexApi: MangaDexApi,
): RemoteMediator<Int, RecentRemoteKeyWithSourceManga>() {

    private val mangaDao = db.sourceMangaDao()
    private val remoteKeysDao = db.recentRemoteKeysDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, RecentRemoteKeyWithSourceManga>
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
                    availableTranslatedLanguage = listOf("en"),
                    order = mapOf("createdAt" to "desc")
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
                            RecentMangaRemoteKey(manga.id, offset + i)
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


internal class RecentMangaRepositoryImpl(
    private val amadeusDatabase: AmadeusDatabase,
    mangaDexApi: MangaDexApi,
): RecentMangaRepository {

    @OptIn(ExperimentalPagingApi::class)
    override val pager = Pager(
        config = PagingConfig(
            pageSize = 60,
            initialLoadSize = 60
        ),
        remoteMediator = RecentMangaRemoteMediator(amadeusDatabase, mangaDexApi),
        pagingSourceFactory = {
            amadeusDatabase.recentRemoteKeysDao().getPagingSource()
        }
    )
}