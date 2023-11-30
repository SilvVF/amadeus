package io.silv.data.manga

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.MangaDexApi
import io.silv.network.model.manga.Manga
import io.silv.network.requests.MangaRequest

class QueryPagingSource(
    private val mangaDexApi: MangaDexApi,
    private val query: MangaRequest
): PagingSource<Int, Manga>() {

    override fun getRefreshKey(state: PagingState<Int, Manga>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Manga> {

        return try {
            val offset = (params.key ?: 0) * params.loadSize

            val response = mangaDexApi.getMangaList(
                query.copy(
                    offset = offset,
                    limit = params.loadSize
                )
            )
                .getOrThrow()


            LoadResult.Page(
                data = response.data,
                prevKey = null,
                nextKey = if (params.loadSize > response.data.size) {
                    null
                } else {
                    (params.key ?: 0) + 1
                },
                itemsBefore = maxOf(0, offset),
                itemsAfter = maxOf(0, response.limit - (offset + params.loadSize)),
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}