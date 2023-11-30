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
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.database.entity.manga.remotekeys.SearchRemoteKey
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest

@OptIn(ExperimentalPagingApi::class)
private class SearchRemoteMediator(
    private val query: SearchMangaResourceQuery,
    private val db: AmadeusDatabase,
    private val mangaDexApi: MangaDexApi,
): RemoteMediator<Int, SourceMangaResource>() {

    private val mangaDao = db.sourceMangaDao()
    private val remoteKeysDao = db.searchRemoteKeysDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, SourceMangaResource>
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
                        remoteKeysDao.getByMangaId(lastItem.id).offset
                    }
                }
            }
            val response = mangaDexApi.getMangaList(
                MangaRequest(
                    offset = offset,
                    limit = state.config.pageSize,
                    includes = listOf("cover_art", "author", "artist"),
                    includedTags = query.includedTags.ifEmpty { null },
                    excludedTags = query.excludedTags.ifEmpty { null },
                    title = query.title,
                    includedTagsMode = query.includedTagsMode,
                    excludedTagsMode = query.excludedTagsMode,
                    status = query.publicationStatus.map { it.name }.ifEmpty { null },
                    availableTranslatedLanguage = query.translatedLanguages.ifEmpty { null },
                    contentRating = query.contentRating.map { it.name }.ifEmpty { null },
                    authors = query.authorIds.ifEmpty { null },
                    artists = query.artistIds.ifEmpty { null },
                    originalLanguage = query.originalLanguages.ifEmpty { null },
                    publicationDemographic = query.demographics.map { it.name }.ifEmpty { null }
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
                            SearchRemoteKey(manga.id, offset + i)
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

internal class SearchMangaRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val amadeusDatabase: AmadeusDatabase,
): SearchMangaRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun pager(query: SearchMangaResourceQuery): Pager<Int, SourceMangaResource> {
        Log.d("SEARCH PAGER", "returning no pager")
       return Pager(
           config = PagingConfig(
               pageSize = 60,
               initialLoadSize = 60 * 2
           ),
           remoteMediator = SearchRemoteMediator(query, amadeusDatabase, mangaDexApi),
           pagingSourceFactory = {
               amadeusDatabase.searchRemoteKeysDao().getPagingSource()
           }
       )
    }
}