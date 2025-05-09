package io.silv.network.model.chapter
import kotlinx.serialization.Serializable

@Serializable
data class ChapterListResponse(
    val result: String,
    val response: String,
    val data: List<ChapterDto> = emptyList(),
    val limit: Int,
    val offset: Int,
    val total: Int,
)
