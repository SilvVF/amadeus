package io.silv.network.model.manga

import io.silv.common.model.Result
import kotlinx.serialization.Serializable

@Serializable
data class MangaByIdResponse(
    val result: Result,
    val response: String,
    val data: MangaDto,
)
