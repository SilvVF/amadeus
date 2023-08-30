package io.silv.amadeus.manga_usecase

import io.silv.ktor_response_mapper.ApiResponse
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.MangaListResponse
import io.silv.manga.network.mangadex.requests.MangaRequest

/**
 * Fetches a manga from the [MangaDexApi] by id and returns it as an ApiResponse.
 * The manga is fetched with includes = listOf("cover_art", "author", "artist").
 * Always fetches from the network.
 */
class GetMangaById(
    private val mangaDexApi: MangaDexApi,
) {

    suspend operator fun invoke(id: String): ApiResponse<MangaListResponse> {
        return mangaDexApi.getMangaList(
            MangaRequest(
                ids = listOf(id),
                includes = listOf("cover_art", "author", "artist")
            )
        )
    }
}