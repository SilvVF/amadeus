package io.silv.data.manga

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.skydoves.sandwich.getOrThrow
import io.silv.common.model.MangaResource
import io.silv.domain.manga.model.MangaUpdate
import io.silv.domain.manga.repository.MangaRepository
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime

class QueryPagingSource(
    private val mangaDexApi: MangaDexApi,
    private val query: MangaRequest,
    private val mangaRepository: MangaRepository,
): PagingSource<Int, MangaResource>() {

    override fun getRefreshKey(state: PagingState<Int, MangaResource>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MangaResource> {

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

                val updates = result.data.map(MangaMapper::dtoToUpdate)

                mangaRepository.upsertManga(updates)

                updates.map { it.toResource() } to result.limit
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

    private fun MangaUpdate.toResource(): MangaResource {
        val update = this
        return object : MangaResource {
            override val id: String = update.id
            override val coverArt: String = update.coverArt
            override val title: String = update.title
            override val version: Int = update.version
            override val createdAt: LocalDateTime = update.createdAt
            override val updatedAt: LocalDateTime = update.updatedAt
        }
    }
}