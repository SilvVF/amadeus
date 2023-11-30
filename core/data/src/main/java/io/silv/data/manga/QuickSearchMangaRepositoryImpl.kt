package io.silv.data.manga

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import io.silv.common.coroutine.suspendRunCatching
import io.silv.data.mappers.toSourceManga
import io.silv.database.AmadeusDatabase
import io.silv.database.dao.remotekeys.QuickSearchRemoteKeyWithManga
import io.silv.database.dao.remotekeys.QuickSearchRemoteKeysDao
import io.silv.database.entity.manga.remotekeys.QuickSearchRemoteKey
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest

@OptIn(ExperimentalPagingApi::class)
private class QuickSearchRemoteMediator(
    private val query: String,
    private val db: AmadeusDatabase,
    private val mangaDexApi: MangaDexApi,
): RemoteMediator<Int, QuickSearchRemoteKeyWithManga>() {

    private val mangaDao = db.sourceMangaDao()
    private val remoteKeysDao = db.quickSearchRemoteKeysDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int,  QuickSearchRemoteKeyWithManga>
    ): MediatorResult {
        return suspendRunCatching {

            val offset = when(loadType) {
                LoadType.REFRESH -> 0
                LoadType.PREPEND -> return@suspendRunCatching MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                    lastItem?.key?.offset ?: 0
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
                    remoteKeysDao.clear()
                }

                val entities = response.data.mapIndexed { i, manga ->
                    manga.toSourceManga().also {

                        remoteKeysDao.insert(
                            QuickSearchRemoteKey(manga.id, offset + state.config.pageSize)
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

internal class QuickSearchMangaRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val quickSearchRemoteKeysDao: QuickSearchRemoteKeysDao,
    private val amadeusDatabase: AmadeusDatabase,
): QuickSearchMangaRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun pager(query: String) = Pager(
        config = PagingConfig(
            pageSize = 60,
            initialLoadSize = 60
        ),
        remoteMediator = QuickSearchRemoteMediator(query, amadeusDatabase, mangaDexApi),
        pagingSourceFactory = {
            quickSearchRemoteKeysDao.getPagingSource()
        }
    )
}