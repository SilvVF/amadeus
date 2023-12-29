package io.silv.network.model.manga

import io.silv.common.model.Result
import kotlinx.serialization.Serializable

@Serializable
data class MangaListResponse(
    val result: Result,
    val response: String = "",
    val data: List<MangaDto>,
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0,
)
