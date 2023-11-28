package io.silv.network

import ChapterListResponse
import io.silv.common.AmadeusDispatchers
import io.silv.ktor_response_mapper.ApiResponse
import io.silv.ktor_response_mapper.ApiResponse.Success
import io.silv.ktor_response_mapper.client.KSandwichClient
import io.silv.ktor_response_mapper.client.get
import io.silv.ktor_response_mapper.suspendOnSuccess
import io.silv.network.model.chapter.ChapterImageResponse
import io.silv.network.model.list.UserIdListResponse
import io.silv.network.model.manga.MangaByIdResponse
import io.silv.network.model.manga.MangaListResponse
import io.silv.network.model.author.AuthorListResponse
import io.silv.network.model.cover.CoverArtListResponse
import io.silv.network.model.tags.TagResponse
import io.silv.network.model.statistics.StatisticsByMangaIdResponse
import io.silv.network.requests.AuthorListRequest
import io.silv.network.requests.ChapterListRequest
import io.silv.network.requests.CoverArtRequest
import io.silv.network.requests.MangaByIdRequest
import io.silv.network.requests.MangaFeedRequest
import io.silv.network.requests.MangaRequest
import io.silv.network.requests.query.createQuery
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class MangaDexApi(
    private val client: KSandwichClient,
    private val dispatchers: AmadeusDispatchers
) {
    private val mangaDexUrl = "https://api.mangadex.org"

    private val cache = ConcurrentHashMap<String, Pair<Success<*>, Long>>()
    private var insertNumber: Long = 0

    private suspend inline fun <reified T : Any> KSandwichClient.getWithCache(request: String): ApiResponse<T> {
        cache[request]?.let { (success, _) ->
            val response = success as? Success<*>
            response
        }
        return this.get<T>(request).also { apiResponse ->
            apiResponse.suspendOnSuccess {
                cache[request] = this to insertNumber++
                if (cache.size >= 20) {
                    cache.entries
                        .sortedBy { (_, respToInsertNum) -> respToInsertNum.second }
                        .take(10)
                        .forEach { (k, _) ->
                            cache.remove(k)
                        }
                }
            }
        }
    }

    suspend fun getMangaStatistics(id: String) = withContext(dispatchers.io) {
        client.getWithCache<StatisticsByMangaIdResponse>("$mangaDexUrl/statistics/manga/$id")
    }

    suspend fun getUserLists(
        id: String
    ) = withContext(dispatchers.io) {
        client.getWithCache<UserIdListResponse>("$mangaDexUrl/user/$id/list")
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
            .createQueryParams()
            .createQuery("$mangaDexUrl/manga")

        client.getWithCache<MangaListResponse>(request)
    }

    suspend fun getChapterImages(chapterId: String) = withContext(dispatchers.io) {
        client.getWithCache<ChapterImageResponse>("$mangaDexUrl/at-home/server/$chapterId")
    }
}

