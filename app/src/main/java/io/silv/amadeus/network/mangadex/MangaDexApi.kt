package io.silv.amadeus.network.mangadex

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.silv.amadeus.AmadeusDispatchers
import io.silv.amadeus.network.mangadex.models.cover.CoverArtByIdResponse
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

    private suspend fun getCoverArtList(
        coverArtListRequest: CoverArtListRequest = CoverArtListRequest(),
    ) = withContext(dispatchers.io) {
        val request = coverArtListRequest
            .createQueryParams()
            .createQuery("https://api.mangadex.org/cover")
        client.get(
            urlString = request
        )
            .body<CoverArtListResponse>()
    }

    private suspend fun getCoverArtById(
        coverArtByIdRequest: CoverArtByIdRequest = CoverArtByIdRequest(),
        mangaOrCoverId: String
    ) = withContext(dispatchers.io) {
        val request = coverArtByIdRequest
            .createQueryParams()
            .createQuery("https://api.mangadex.org/cover/$mangaOrCoverId")
        client.get(
            urlString = request
        )
            .body<CoverArtByIdResponse>()
    }

    suspend fun getMangaAggregate(
        mangaId: String,
        mangaAggregateRequest: MangaAggregateRequest = MangaAggregateRequest()
    ) = withContext(dispatchers.io) {
        val request = mangaAggregateRequest
            .createQueryParams()
            .createQuery("https://api.mangadex.org/manga/$mangaId/aggregate")

        client.get(
            urlString = request
        )
            .body<MangaAggregateResponse>()
    }

    suspend fun getMangaById(
        id: String,
        mangaByIdRequest: MangaByIdRequest = MangaByIdRequest()
    ) = withContext(dispatchers.io) {
        val request = mangaByIdRequest
            .createQueryParams()
            .createQuery("https://api.mangadex.org/manga/$id")
        client.get(
            urlString = request
        )
            .body<MangaByIdResponse>()
    }

    suspend fun getMangaList(
        mangaRequest: MangaRequest = MangaRequest()
    ) = withContext(dispatchers.io) {
        val request = mangaRequest
            .createQueryParams()
            .createQuery("https://api.mangadex.org/manga")
        client.get(
            urlString = request
        )
            .body<MangaListResponse>()
    }
}
