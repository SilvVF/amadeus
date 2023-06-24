package io.silv.amadeus.network.mangadex.models.manga

import io.silv.amadeus.network.mangadex.models.Result
import kotlinx.serialization.Serializable

@Serializable
data class MangaByIdResponse(
    val result: Result,
    val response: String,
    val data: Manga
)