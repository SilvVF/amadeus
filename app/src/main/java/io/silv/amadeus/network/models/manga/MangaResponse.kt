package io.silv.amadeus.network.models.manga

import io.silv.amadeus.network.models.Result
import kotlinx.serialization.Serializable

@Serializable
data class MangaResponse(
    val result: Result,
    val response: String,
    val data: List<Manga>,
    val limit: Int,
    val offset: Int,
    val total: Int,
)
