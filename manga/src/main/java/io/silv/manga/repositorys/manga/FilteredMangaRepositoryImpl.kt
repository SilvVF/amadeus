package io.silv.manga.repositorys.manga

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
import io.silv.manga.repositorys.timeString
import io.silv.manga.repositorys.toFilteredMangaResource
import io.silv.manga.local.AmadeusDatabase
import io.silv.manga.local.dao.FilteredMangaResourceDao
import io.silv.manga.local.entity.manga_resource.FilteredMangaResource
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.requests.MangaRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

@OptIn(ExperimentalPagingApi::class)
private class FilteredMangaRemoteMediator(
    private val query: FilteredResourceQuery,
    private val db: AmadeusDatabase,
    private val mangaDexApi: MangaDexApi,
): RemoteMediator<Int, FilteredMangaResource>() {

    private val dao = db.filteredMangaResourceDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, FilteredMangaResource>
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
                    availableTranslatedLanguage = listOf("en"),
                    hasAvailableChapters = true,
                    order = mapOf("followedCount" to "desc"),
                    includedTags = listOf(query.tagId),
                    includedTagsMode = MangaRequest.TagsMode.AND,
                    createdAtSince = query.timePeriod.timeString()
                )
            )
                .getOrThrow()

            db.withTransaction {
                if(loadType == LoadType.REFRESH) {
                    dao.deleteAll()
                }
                val entities = response.data.map { manga ->
                    manga.toFilteredMangaResource().copy(offset = offset)
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


data class FilteredResourceQuery(
    val tagId: String,
    val timePeriod: FilteredMangaRepository.TimePeriod = FilteredMangaRepository.TimePeriod.AllTime
)

internal class FilteredMangaRepositoryImpl(
    private val resourceDao: FilteredMangaResourceDao,
    private val amadeusDatabase: AmadeusDatabase,
    private val mangaDexApi: MangaDexApi,
    private val dispatchers: AmadeusDispatchers,
): FilteredMangaRepository {


    @OptIn(ExperimentalPagingApi::class)
    override fun pager(query: FilteredResourceQuery)  = Pager(
        config = PagingConfig(pageSize = 60),
        remoteMediator = FilteredMangaRemoteMediator(query, amadeusDatabase, mangaDexApi),
        pagingSourceFactory = {
            resourceDao.pagingSource()
        }
    )

    override fun observeMangaResourceById(id: String): Flow<FilteredMangaResource?> {
        return resourceDao.observeFilteredMangaResourceById(id).flowOn(dispatchers.io)
    }

    override fun observeAllMangaResources(): Flow<List<FilteredMangaResource>> {
        return resourceDao.getFilteredMangaResources().flowOn(dispatchers.io)
    }
}