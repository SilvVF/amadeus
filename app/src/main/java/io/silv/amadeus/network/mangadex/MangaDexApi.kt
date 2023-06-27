package io.silv.amadeus.network.mangadex

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.silv.amadeus.AmadeusDispatchers
import io.silv.amadeus.network.mangadex.models.cover.CoverResponse
import io.silv.amadeus.network.mangadex.models.cover.CoverArtListResponse
import io.silv.amadeus.network.mangadex.models.manga.MangaAggregateResponse
import io.silv.amadeus.network.mangadex.models.manga.MangaByIdResponse
import io.silv.amadeus.network.mangadex.models.manga.MangaListResponse
import io.silv.amadeus.network.mangadex.requests.CoverArtByIdRequest
import io.silv.amadeus.network.mangadex.requests.CoverArtListRequest
import io.silv.amadeus.network.mangadex.requests.MangaAggregateRequest
import io.silv.amadeus.network.mangadex.requests.MangaByIdRequest
import io.silv.amadeus.network.mangadex.requests.MangaRequest
import io.silv.amadeus.network.mangadex.requests.util.createQuery
import io.silv.amadeus.network.mangadex.requests.util.createQueryParams
import kotlinx.coroutines.withContext

class MangaDexApi(
    private val client: HttpClient,
    private val dispatchers: AmadeusDispatchers
) {
    private val mangaDexUrl = "https://api.mangadex.org"

    suspend fun getCoverArtList(
        coverArtListRequest: CoverArtListRequest = CoverArtListRequest(),
    ) = withContext(dispatchers.io) {
        val request = coverArtListRequest
            .createQueryParams()
            .createQuery("$mangaDexUrl/cover")
        client.get(
            urlString = request
        )
            .body<CoverArtListResponse>()
    }

    suspend fun getCoverArtById(
        mangaOrCoverId: String,
        coverArtByIdRequest: CoverArtByIdRequest = CoverArtByIdRequest(),
    ) = withContext(dispatchers.io) {
        val request = coverArtByIdRequest
            .createQueryParams().also { println(it) }
            .createQuery("$mangaDexUrl/cover/$mangaOrCoverId").also { println(it) }

        client.get(request)
            .body<CoverResponse>()
    }

    suspend fun getMangaAggregate(
        mangaId: String,
        mangaAggregateRequest: MangaAggregateRequest = MangaAggregateRequest()
    ) = withContext(dispatchers.io) {
        val request = mangaAggregateRequest
            .createQueryParams()
            .createQuery("$mangaDexUrl/manga/$mangaId/aggregate")

        client.get(request)
            .body<MangaAggregateResponse>()
    }

    suspend fun getMangaById(
        id: String,
        mangaByIdRequest: MangaByIdRequest = MangaByIdRequest()
    ) = withContext(dispatchers.io) {
        val request = mangaByIdRequest
            .createQueryParams()
            .createQuery("$mangaDexUrl/manga/$id")
        client.get(request)
            .body<MangaByIdResponse>()
    }

    suspend fun getMangaList(
        mangaRequest: MangaRequest = MangaRequest()
    ) = withContext(dispatchers.io) {
        val request = mangaRequest
            .createQueryParams().also { println(it) }
            .createQuery("$mangaDexUrl/manga")
        println(request)
        client.get(request)
            .body<MangaListResponse>()
    }
}
