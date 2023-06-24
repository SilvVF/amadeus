package io.silv.amadeus.network.mangadex.models.manga

import io.silv.amadeus.network.mangadex.models.Result
import kotlinx.serialization.Serializable

@Serializable
data class MangaListResponse(
    val result: Result,
    val response: String,
    val data: List<Manga>,
    val limit: Int,
    val offset: Int,
    val total: Int,
)
