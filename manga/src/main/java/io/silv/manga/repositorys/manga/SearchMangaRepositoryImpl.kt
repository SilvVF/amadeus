package io.silv.manga.repositorys.manga

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.repositorys.suspendRunCatching
import io.silv.manga.repositorys.toSearchMangaResource
import io.silv.manga.local.AmadeusDatabase
import io.silv.manga.local.dao.SearchMangaResourceDao
import io.silv.manga.local.entity.manga_resource.SearchMangaResource
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.requests.MangaRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

@OptIn(ExperimentalPagingApi::class)
private class SearchRemoteMediator(
    private val query: SearchMangaResourceQuery,
    private val db: AmadeusDatabase,
    private val mangaDexApi: MangaDexApi,
): RemoteMediator<Int, SearchMangaResource>() {

    private val dao = db.searchMangaResourceDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, SearchMangaResource>
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
                    includes = listOf("cover_art","author", "artist"),
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
                    dao.deleteAll()
                }
                val entities = response.data.map { manga ->
                    manga.toSearchMangaResource().copy(offset = offset)
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

internal class SearchMangaRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val amadeusDatabase: AmadeusDatabase,
    private val mangaResourceDao: SearchMangaResourceDao,
    private val dispatchers: AmadeusDispatchers,
): SearchMangaRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun pager(query: SearchMangaResourceQuery): Pager<Int, SearchMangaResource> {
        Log.d("SEARCH PAGER", "returning no pager")
       return Pager(
           config = PagingConfig(
               pageSize = 60,
               initialLoadSize = 60 * 2
           ),
           remoteMediator = SearchRemoteMediator(query, amadeusDatabase, mangaDexApi),
           pagingSourceFactory = { mangaResourceDao.pagingSource() }
       )
    }

    override fun observeMangaResourceById(id: String): Flow<SearchMangaResource?> {
        return mangaResourceDao.observeSearchMangaResourceById(id).flowOn(dispatchers.io)
    }

    override fun observeAllMangaResources(): Flow<List<SearchMangaResource>> {
        return mangaResourceDao.observeAllSearchMangaResources().flowOn(dispatchers.io)
    }
}