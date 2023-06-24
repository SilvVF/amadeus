package io.silv.amadeus.network.mangadex.models.manga

import kotlinx.serialization.Serializable

@Serializable
data class MangaAggregateResponse(
    val result: String,
    val volumes: Map<String, MangaAggregateData>
) {

    @Serializable
    data class MangaAggregateData(
        val volume: String,
        val count: Int,
        val chapters: Map<String, ChapterData>
    ) {

        @Serializable
        data class ChapterData(
            val chapter: String,
            val id: String,
            val others: List<String>,
            val count: Int,
        )
    }
}