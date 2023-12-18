package io.silv.network

import ChapterListResponse
import com.skydoves.sandwich.ktor.getApiResponse
import io.ktor.client.HttpClient
import io.silv.common.AmadeusDispatchers
import io.silv.network.model.author.AuthorListResponse
import io.silv.network.model.chapter.ChapterImageResponse
import io.silv.network.model.cover.CoverArtListResponse
import io.silv.network.model.list.UserIdListResponse
import io.silv.network.model.manga.MangaByIdResponse
import io.silv.network.model.manga.MangaListResponse
import io.silv.network.model.statistics.StatisticsByMangaIdResponse
import io.silv.network.model.tags.TagResponse
import io.silv.network.requests.AuthorListRequest
import io.silv.network.requests.ChapterListRequest
import io.silv.network.requests.CoverArtRequest
import io.silv.network.requests.MangaByIdRequest
import io.silv.network.requests.MangaFeedRequest
import io.silv.network.requests.MangaRequest
import io.silv.network.requests.query.createQuery
import kotlinx.coroutines.withContext

class MangaDexApi(
    private val client: HttpClient,
    private val dispatchers: AmadeusDispatchers,
) {
    private val mangaDexUrl = "https://api.mangadex.org"

    suspend fun getMangaStatistics(id: String) =
        withContext(dispatchers.io) {
            client.getApiResponse<StatisticsByMangaIdResponse>("$mangaDexUrl/statistics/manga/$id")
        }

    suspend fun getUserLists(id: String) =
        withContext(dispatchers.io) {
            client.getApiResponse<UserIdListResponse>("$mangaDexUrl/user/$id/list")
        }

    suspend fun getAuthorList(authorListRequest: AuthorListRequest) =
        withContext(dispatchers.io) {
            val request =
                authorListRequest
                    .createQueryParams()
                    .createQuery("$mangaDexUrl/author")

            client.getApiResponse<AuthorListResponse>(request)
        }

    suspend fun getCoverArtList(coverArtRequest: CoverArtRequest) =
        withContext(dispatchers.io) {
            val request =
                coverArtRequest
                    .createQueryParams()
                    .createQuery("$mangaDexUrl/cover")

            client.getApiResponse<CoverArtListResponse>(request)
        }

    suspend fun getChapterData(chapterListRequest: ChapterListRequest) =
        withContext(dispatchers.io) {
            val request = chapterListRequest.createQueryParams().createQuery("$mangaDexUrl/chapter")

            client.getApiResponse<ChapterListResponse>(request)
        }

    suspend fun getMangaFeed(
        mangaId: String,
        mangaFeedRequest: MangaFeedRequest = MangaFeedRequest(),
    ) = withContext(dispatchers.io) {
        val request =
            mangaFeedRequest
                .createQueryParams()
                .createQuery("$mangaDexUrl/manga/$mangaId/feed")

        client.getApiResponse<ChapterListResponse>(request)
    }

    suspend fun getTagList() =
        withContext(dispatchers.io) {
            client.getApiResponse<TagResponse>("$mangaDexUrl/manga/tag")
        }

    suspend fun getMangaById(
        id: String,
        mangaByIdRequest: MangaByIdRequest = MangaByIdRequest(),
    ) = withContext(dispatchers.io) {
        val request =
            mangaByIdRequest
                .createQueryParams()
                .createQuery("$mangaDexUrl/manga/$id")

        client.getApiResponse<MangaByIdResponse>(request)
    }

    suspend fun getMangaList(mangaRequest: MangaRequest = MangaRequest()) =
        withContext(dispatchers.io) {
            val request =
                mangaRequest
                    .createQueryParams()
                    .createQuery("$mangaDexUrl/manga")

            client.getApiResponse<MangaListResponse>(request)
        }

    suspend fun getChapterImages(chapterId: String) =
        withContext(dispatchers.io) {
            client.getApiResponse<ChapterImageResponse>("$mangaDexUrl/at-home/server/$chapterId")
        }
}
