package io.silv.data.manga

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.skydoves.sandwich.getOrThrow
import io.silv.data.mappers.toEntity
import io.silv.database.AmadeusDatabase
import io.silv.domain.manga.model.Manga
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QueryPagingSource(
    private val mangaDexApi: MangaDexApi,
    private val query: MangaRequest,
    private val db: AmadeusDatabase,
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
                    .map { it.toEntity() }
                    .also { db.sourceMangaDao().insertAll(it) }
                    .map { it.let(MangaMapper::mapManga) } to result.limit
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