package io.silv.amadeus.network.mangadex

import ChapterListResponse
import io.silv.amadeus.AmadeusDispatchers
import io.silv.amadeus.network.mangadex.models.chapter.Chapter
import io.silv.amadeus.network.mangadex.models.chapter.ChapterImageResponse
import io.silv.amadeus.network.mangadex.models.cover.Cover
import io.silv.amadeus.network.mangadex.models.cover.CoverArtListResponse
import io.silv.amadeus.network.mangadex.models.manga.MangaAggregateResponse
import io.silv.amadeus.network.mangadex.models.manga.MangaByIdResponse
import io.silv.amadeus.network.mangadex.models.manga.MangaListResponse
import io.silv.amadeus.network.mangadex.requests.CoverArtRequest
import io.silv.amadeus.network.mangadex.requests.MangaAggregateRequest
import io.silv.amadeus.network.mangadex.requests.MangaByIdRequest
import io.silv.amadeus.network.mangadex.requests.MangaFeedRequest
import io.silv.amadeus.network.mangadex.requests.MangaRequest
import io.silv.amadeus.network.mangadex.requests.query.createQuery
import io.silv.amadeus.network.mangadex.requests.query.createQueryParams
import io.silv.ktor_response_mapper.client.KSandwichClient
import io.silv.ktor_response_mapper.client.get
import kotlinx.coroutines.withContext

class MangaDexApi(
    private val client: KSandwichClient,
    private val dispatchers: AmadeusDispatchers
) {
    private val mangaDexUrl = "https://api.mangadex.org"

    suspend fun getCoverArtList(
        coverArtRequest: CoverArtRequest,
    ) = withContext(dispatchers.io) {
        val request = coverArtRequest
            .createQueryParams()
            .createQuery("$mangaDexUrl/cover")
        println(request)
        client.get<CoverArtListResponse>(request)
    }

    suspend fun getChapterData(chapterId: String) = withContext(dispatchers.io) {
        client.get<Chapter>("$mangaDexUrl/chapter/$chapterId")
    }

    suspend fun getMangaFeed(
        mangaId: String,
        mangaFeedRequest: MangaFeedRequest = MangaFeedRequest()
    ) = withContext(dispatchers.io) {
        val request = mangaFeedRequest
            .createQueryParams()
            .createQuery("$mangaDexUrl/manga/$mangaId/feed")

        client.get<ChapterListResponse>(request)
    }

    suspend fun getCoverArtById(
        mangaOrCoverId: String,
    ) = withContext(dispatchers.io) {
        client.get<Cover>("$mangaDexUrl/cover/$mangaOrCoverId")
    }

    suspend fun getMangaAggregate(
        mangaId: String,
        mangaAggregateRequest: MangaAggregateRequest = MangaAggregateRequest()
    ) = withContext(dispatchers.io) {
        val request = mangaAggregateRequest
            .createQueryParams()
            .createQuery("$mangaDexUrl/manga/$mangaId/aggregate")
        println(request)
        client.get<MangaAggregateResponse>(request)
    }

    suspend fun getMangaById(
        id: String,
        mangaByIdRequest: MangaByIdRequest = MangaByIdRequest()
    ) = withContext(dispatchers.io) {
        val request = mangaByIdRequest
            .createQueryParams()
            .createQuery("$mangaDexUrl/manga/$id")
        println(request)
        client.get<MangaByIdResponse>(request)
    }

    suspend fun getMangaList(
        mangaRequest: MangaRequest = MangaRequest()
    ) = withContext(dispatchers.io) {
        val request = mangaRequest
            .createQueryParams().also { println(it) }
            .createQuery("$mangaDexUrl/manga")
        println(request)
        client.get<MangaListResponse>(request)
    }

    suspend fun getChapterImages(chapterId: String) = withContext(dispatchers.io) {
        client.get<ChapterImageResponse>(urlString = "$mangaDexUrl/at-home/server/$chapterId".also { println(it) })
    }
}

