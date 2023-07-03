package io.silv.manga.network.mangadex.models.manga

import io.silv.manga.network.mangadex.models.Result
import kotlinx.serialization.Serializable

@Serializable
data class MangaListResponse(
    val result: Result,
    val response: String = "",
    val data: List<Manga>,
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0,
)
