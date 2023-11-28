package io.silv.network.model.chapter

import kotlinx.serialization.Serializable

@Serializable
data class ChapterImageResponse(
    val result: String,
    val baseUrl: String,
    val chapter: Chapter
) {

    @Serializable
    data class Chapter(
        val hash: String,
        val data: List<String>,
        val dataSaver: List<String>
    )
}
