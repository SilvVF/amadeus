package io.silv.data.manga

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.model.TimePeriod
import io.silv.data.mappers.timeString
import io.silv.data.mappers.toSourceManga
import io.silv.database.AmadeusDatabase
import io.silv.database.dao.remotekeys.FilteredRemoteKeysDao
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.database.entity.manga.remotekeys.FilteredRemoteKey
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest

@OptIn(ExperimentalPagingApi::class)
private class FilteredMangaRemoteMediator(
    private val query: FilteredResourceQuery,
    private val db: AmadeusDatabase,
    private val mangaDexApi: MangaDexApi,
): RemoteMediator<Int, SourceMangaResource>() {

    private val remoteKeysDao = db.filteredRemoteKeysDao()
    private val mangaDao = db.sourceMangaDao()

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
                    availableTranslatedLanguage = listOf("en"),
                    hasAvailableChapters = true,
                    order = mapOf("followedCount" to "desc"),
                    includedTags = listOf(query.tagId),
                    includedTagsMode = MangaRequest.TagsMode.AND,
                    createdAtSince = query.timePeriod.timeString(),
                    contentRating = listOf("safe")
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
                            FilteredRemoteKey(manga.id, offset + i)
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


data class FilteredResourceQuery(
    val tagId: String,
    val timePeriod: TimePeriod = TimePeriod.AllTime
)

internal class FilteredMangaRepositoryImpl(
    private val amadeusDatabase: AmadeusDatabase,
    private val filteredRemoteKeysDao: FilteredRemoteKeysDao,
    private val mangaDexApi: MangaDexApi,
): FilteredMangaRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun pager(query: FilteredResourceQuery)  = Pager(
        config = PagingConfig(pageSize = 60),
        remoteMediator = FilteredMangaRemoteMediator(
            query,
            amadeusDatabase,
            mangaDexApi
        ),
        pagingSourceFactory = {
            filteredRemoteKeysDao.getPagingSource()
        }
    )
}