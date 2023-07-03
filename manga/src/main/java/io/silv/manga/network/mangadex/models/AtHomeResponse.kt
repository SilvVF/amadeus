package io.silv.manga.network.mangadex.models

import kotlinx.serialization.Serializable

@Serializable
data class AtHomeResponse(
    val result: String,
    /**
     * The base URL to construct final image URLs from.
     * The URL returned is valid for the requested chapter only,
     * and for a duration of 15 minutes from the time of the response.
     */
    val baseUrl: String,
    val chapter: AtHomeData
) {

    @Serializable
    data class AtHomeData(
        val hash: String,
        val data: List<String>,
        val dataSaver: List<String>
    )
}