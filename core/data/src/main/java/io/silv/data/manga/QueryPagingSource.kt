package io.silv.data.manga

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.silv.data.mappers.toSourceManga
import io.silv.database.dao.SourceMangaDao
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QueryPagingSource(
    private val mangaDexApi: MangaDexApi,
    private val query: MangaRequest,
    private val sourceMangaDao: SourceMangaDao,
): PagingSource<Int, SourceMangaResource>() {

    override fun getRefreshKey(state: PagingState<Int, SourceMangaResource>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SourceMangaResource> {

        return try {

            val offset = (params.key ?: 0) * params.loadSize

            val (response, limit) = withContext(Dispatchers.IO) {

                val result = mangaDexApi.getMangaList(
                    query.copy(
                        offset = offset,
                        includes = listOf("cover_art", "author", "artist"),
                        limit = params.loadSize
                    )
                )
                    .getOrThrow()

                result.data
                    .map { it.toSourceManga() }
                    .also { sourceMangaDao.insertAll(it) } to result.limit
            }


            LoadResult.Page(
                data = response,
                prevKey = null,
                nextKey = if (params.loadSize > response.size) {
                    null
                } else {
                    (params.key ?: 0) + 1
                },
                itemsBefore = maxOf(0, offset),
                itemsAfter = maxOf(0, limit - (offset + params.loadSize)),
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}