package io.silv.manga.network.mangadex

import ChapterListResponse
import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.ApiResponse
import io.silv.ktor_response_mapper.client.KSandwichClient
import io.silv.ktor_response_mapper.client.get
import io.silv.ktor_response_mapper.suspendOnSuccess
import io.silv.manga.network.mangadex.models.chapter.Chapter
import io.silv.manga.network.mangadex.models.chapter.ChapterImageResponse
import io.silv.manga.network.mangadex.models.cover.Cover
import io.silv.manga.network.mangadex.models.cover.CoverArtListResponse
import io.silv.manga.network.mangadex.models.manga.MangaAggregateResponse
import io.silv.manga.network.mangadex.models.manga.MangaByIdResponse
import io.silv.manga.network.mangadex.models.manga.MangaListResponse
import io.silv.manga.network.mangadex.requests.CoverArtRequest
import io.silv.manga.network.mangadex.requests.MangaAggregateRequest
import io.silv.manga.network.mangadex.requests.MangaByIdRequest
import io.silv.manga.network.mangadex.requests.MangaFeedRequest
import io.silv.manga.network.mangadex.requests.MangaRequest
import io.silv.manga.network.mangadex.requests.query.createQuery
import io.silv.manga.network.mangadex.requests.query.createQueryParams
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class MangaDexApi(
    private val client: KSandwichClient,
    private val dispatchers: AmadeusDispatchers
) {
    private val mangaDexUrl = "https://api.mangadex.org"

    private val cache = ConcurrentHashMap<String, ApiResponse.Success<*>>()

    private suspend inline fun <reified T : Any> KSandwichClient.getWithCache(request: String): ApiResponse<T> {
        cache[request]?.let { success ->
            val response = success as? ApiResponse.Success<T>
            response?.let { return it }
        }
        return this.get<T>(request).also {
            it.suspendOnSuccess {
                cache[request] = this
            }
        }
    }

    suspend fun getCoverArtList(
        coverArtRequest: CoverArtRequest,
    ) = withContext(dispatchers.io) {
        val request = coverArtRequest
            .createQueryParams()
            .createQuery("$mangaDexUrl/cover")
        client.getWithCache<CoverArtListResponse>(request)
    }

    suspend fun getChapterData(chapterId: String) = withContext(dispatchers.io) {
        client.getWithCache<Chapter>("$mangaDexUrl/chapter/$chapterId")
    }

    suspend fun getMangaFeed(
        mangaId: String,
        mangaFeedRequest: MangaFeedRequest = MangaFeedRequest()
    ) = withContext(dispatchers.io) {
        val request = mangaFeedRequest
            .createQueryParams()
            .createQuery("$mangaDexUrl/manga/$mangaId/feed")

        client.getWithCache<ChapterListResponse>(request)
    }

    suspend fun getCoverArtById(
        mangaOrCoverId: String,
    ) = withContext(dispatchers.io) {
        client.getWithCache<Cover>("$mangaDexUrl/cover/$mangaOrCoverId")
    }

    suspend fun getMangaAggregate(
        mangaId: String,
        mangaAggregateRequest: MangaAggregateRequest = MangaAggregateRequest()
    ) = withContext(dispatchers.io) {
        val request = mangaAggregateRequest
            .createQueryParams()
            .createQuery("$mangaDexUrl/manga/$mangaId/aggregate")
        println(request)
        client.getWithCache<MangaAggregateResponse>(request)
    }

    suspend fun getMangaById(
        id: String,
        mangaByIdRequest: MangaByIdRequest = MangaByIdRequest()
    ) = withContext(dispatchers.io) {
        val request = mangaByIdRequest
            .createQueryParams()
            .createQuery("$mangaDexUrl/manga/$id")
        client.getWithCache<MangaByIdResponse>(request)
    }

    suspend fun getMangaList(
        mangaRequest: MangaRequest = MangaRequest()
    ) = withContext(dispatchers.io) {
        val request = mangaRequest
            .createQueryParams().also { println(it) }
            .createQuery("$mangaDexUrl/manga")
        println(request)
        client.getWithCache<MangaListResponse>(request)
    }

    suspend fun getChapterImages(chapterId: String) = withContext(dispatchers.io) {
        client.getWithCache<ChapterImageResponse>("$mangaDexUrl/at-home/server/$chapterId")
    }
}
