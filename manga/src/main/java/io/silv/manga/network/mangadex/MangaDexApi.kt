package io.silv.manga.network.mangadex

import ChapterListResponse
import android.util.Log
import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.ApiResponse
import io.silv.ktor_response_mapper.client.KSandwichClient
import io.silv.ktor_response_mapper.client.get
import io.silv.ktor_response_mapper.suspendOnSuccess
import io.silv.manga.network.mangadex.models.author.AuthorListResponse
import io.silv.manga.network.mangadex.models.chapter.ChapterImageResponse
import io.silv.manga.network.mangadex.models.cover.CoverArtListResponse
import io.silv.manga.network.mangadex.models.list.UserIdListResponse
import io.silv.manga.network.mangadex.models.manga.MangaByIdResponse
import io.silv.manga.network.mangadex.models.manga.MangaListResponse
import io.silv.manga.network.mangadex.models.statistics.StatisticsByMangaIdResponse
import io.silv.manga.network.mangadex.models.tags.TagResponse
import io.silv.manga.network.mangadex.requests.AuthorListRequest
import io.silv.manga.network.mangadex.requests.ChapterListRequest
import io.silv.manga.network.mangadex.requests.CoverArtRequest
import io.silv.manga.network.mangadex.requests.MangaByIdRequest
import io.silv.manga.network.mangadex.requests.MangaFeedRequest
import io.silv.manga.network.mangadex.requests.MangaRequest
import io.silv.manga.network.mangadex.requests.query.createQuery
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class MangaDexApi(
    private val client: KSandwichClient,
    private val dispatchers: AmadeusDispatchers
) {
    private val mangaDexUrl = "https://api.mangadex.org"

    private val cache = ConcurrentHashMap<String, Pair<ApiResponse.Success<*>, Long>>()
    private var insertNumber: Long = 0

    private suspend inline fun <reified T : Any> KSandwichClient.getWithCache(request: String): ApiResponse<T> {
        cache[request]?.let {(success, _) ->
            val response = success as? ApiResponse.Success<T>
            response?.let { return it.also { Log.d("MangaDexApi", "Value from cache") } }
        }
        return this.get<T>(request).also {
            it.suspendOnSuccess {
                cache[request] = this to insertNumber++
                if (cache.size >= 40) {
                    cache.entries
                        .sortedBy { it.value.second }
                        .take(20)
                        .forEach { (k, _) ->
                            cache.remove(k)
                        }
                }
            }
        }
    }

    suspend fun getMangaStatistics(id: String) = withContext(dispatchers.io) {
        client.getWithCache<StatisticsByMangaIdResponse>(
            "$mangaDexUrl/statistics/manga/$id"
        )
    }

//    suspend fun getListById(
//        id: String
//    ) = withContext(dispatchers.io) {
//        client.get<ListByIdResponse>("$mangaDexUrl/list/$id")
//    }

    suspend fun getUserLists(
        id: String
    ) = withContext(dispatchers.io) {
        client.getWithCache<UserIdListResponse>("$mangaDexUrl/user/$id/list".also { println(it) })
    }

    suspend fun getAuthorList(
        authorListRequest: AuthorListRequest
    ) = withContext(dispatchers.io) {
        val request = authorListRequest
            .createQueryParams()
            .createQuery("$mangaDexUrl/author")
        client.getWithCache<AuthorListResponse>(request)
    }

    suspend fun getCoverArtList(
        coverArtRequest: CoverArtRequest,
    ) = withContext(dispatchers.io) {
        val request = coverArtRequest
            .createQueryParams()
            .createQuery("$mangaDexUrl/cover")
        client.getWithCache<CoverArtListResponse>(request)
    }


    suspend fun getChapterData(chapterListRequest: ChapterListRequest) = withContext(dispatchers.io) {
        val request = chapterListRequest.createQueryParams().createQuery("$mangaDexUrl/chapter")
        client.getWithCache<ChapterListResponse>(request)
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

//    suspend fun getCoverArtById(
//        mangaOrCoverId: String,
//    ) = withContext(dispatchers.io) {
//        client.getWithCache<Cover>("$mangaDexUrl/cover/$mangaOrCoverId")
//    }

//    suspend fun getMangaAggregate(
//        mangaId: String,
//        mangaAggregateRequest: MangaAggregateRequest = MangaAggregateRequest()
//    ) = withContext(dispatchers.io) {
//        val request = mangaAggregateRequest
//            .createQueryParams()
//            .createQuery("$mangaDexUrl/manga/$mangaId/aggregate")
//        client.getWithCache<MangaAggregateResponse>(request)
//    }

    suspend fun getTagList() = withContext(dispatchers.io) {
        client.get<TagResponse>("$mangaDexUrl/manga/tag")
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
        client.getWithCache<MangaListResponse>(request)
    }

    suspend fun getChapterImages(chapterId: String) = withContext(dispatchers.io) {
        client.getWithCache<ChapterImageResponse>("$mangaDexUrl/at-home/server/$chapterId")
    }
}

