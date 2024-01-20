package io.silv.data.manga

import com.skydoves.sandwich.mapSuccess
import io.silv.model.MangaStats
import io.silv.network.MangaDexApi
import io.silv.network.requests.CoverArtRequest

/**
 * Fetches the [MangaStats] for the given manga id from the Manga Dex api.
 * This always fetches from the network.
 */
class GetMangaCoverArtById(
    private val mangaDexApi: MangaDexApi,
) {
    suspend fun await(id: String) =
        mangaDexApi.getCoverArtList(
            CoverArtRequest(
                limit = 100,
                manga = listOf(id)
            )
        )
            .mapSuccess {
                data.filter { it.type == "cover_art" }.map {
                    "https://uploads.mangadex.org/covers/$id/${it.attributes.fileName}"
                }
            }
}
