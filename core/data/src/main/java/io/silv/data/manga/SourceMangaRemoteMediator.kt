package io.silv.data.manga

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import io.silv.common.coroutine.suspendRunCatching
import io.silv.data.mappers.toSourceManga
import io.silv.database.AmadeusDatabase
import io.silv.database.dao.remotekeys.RemoteKeyWithManga
import io.silv.database.entity.manga.remotekeys.RemoteKey
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest

@OptIn(ExperimentalPagingApi::class)
class SourceMangaRemoteMediator(
    private val db: AmadeusDatabase,
    private val mangaDexApi: MangaDexApi,
    private val query: String,
    private val mangaRequest: MangaRequest
): RemoteMediator<Int, RemoteKeyWithManga>() {

    private val mangaDao = db.sourceMangaDao()
    private val remoteKeysDao = db.remoteKeyDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, RemoteKeyWithManga>
    ): MediatorResult {
        Log.d("SourceMediator", "loading...")
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
                mangaRequest.copy(
                    offset = offset,
                    limit = state.config.pageSize,
                    includes = (mangaRequest.includes ?: emptyList()) + listOf("cover_art", "author", "artist")
                )
            ).getOrThrow()

            Log.d("SourceMediator", "response items count: ${response.data.size}")

            db.withTransaction {
                if(loadType == LoadType.REFRESH) {
                    remoteKeysDao.clearByQuery(query)
                }
                response.data.forEachIndexed { i, manga ->
                    Log.d("SourceMediator", "inserting manga ${manga.id}")
                    mangaDao.insert(manga.toSourceManga())
                    Log.d("SourceMediator", "inserting key manga_id ${manga.id} @offset ${offset + i}")
                    remoteKeysDao.insert(
                        RemoteKey(
                            mangaId = manga.id,
                            offset = offset + i,
                            queryId = query
                        )
                    )
                }
            }

            Log.d("SourceMediator", "end")

            MediatorResult.Success(
                endOfPaginationReached = offset + response.data.size >= response.total
            )
        }.getOrElse {
            Log.d("SourceMediator", "failed  ${it.stackTraceToString()}")
            MediatorResult.Error(it)
        }
    }
}