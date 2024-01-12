package io.silv.data.util

import ChapterListResponse
import com.skydoves.sandwich.getOrThrow
import io.silv.common.AmadeusDispatchers
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.model.Order
import io.silv.common.model.OrderBy
import io.silv.network.MangaDexApi
import io.silv.network.model.chapter.ChapterDto
import io.silv.network.requests.MangaFeedRequest
import kotlinx.coroutines.withContext

internal class GetChapterList(
    private val mangaDexApi: MangaDexApi,
    private val dispatchers: AmadeusDispatchers
) {

    suspend fun await(id: String): List<ChapterDto> {
        return fetchChapterList(id)
    }

    private suspend fun fetchChapterList(mangaId: String): List<ChapterDto> = withContext(dispatchers.io) {

        val initialList = getInitialChapterList(mangaId).getOrThrow()

        initialList.data + getRestOfChapters(initialList, mangaId)
    }

    private suspend fun getInitialChapterList(
        mangaId: String,
        langs: List<String> = listOf("en")
    ) = suspendRunCatching {
        mangaDexApi.getMangaFeed(
            mangaId = mangaId,
            mangaFeedRequest = MangaFeedRequest(
                translatedLanguage = langs,
                offset = 0,
                limit = 500,
                includes = listOf("scanlation_group", "user"),
                order = mapOf(Order.chapter to OrderBy.asc)
            )
        )
            .getOrThrow()
    }

    private suspend fun getRestOfChapters(
        response: ChapterListResponse,
        mangaId: String,
        langs: List<String> = listOf("en")
    ): List<ChapterDto> {
        return withContext(dispatchers.io) {
            val count = (response.total / response.limit)
            (1..count).map {
                mangaDexApi.getMangaFeed(
                    mangaId = mangaId,
                    mangaFeedRequest = MangaFeedRequest(
                        translatedLanguage = langs,
                        offset = it * response.limit,
                        limit = 500,
                        includes = listOf("scanlation_group", "user"),
                        order = mapOf(Order.chapter to OrderBy.asc)
                    )
                )
                    .getOrThrow()
                    .data
            }
                .flatten()
        }
    }
}