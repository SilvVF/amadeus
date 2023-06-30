package io.silv.amadeus.network.mangadex.models.chapter

import io.silv.amadeus.network.mangadex.models.Result
import kotlinx.serialization.Serializable


@Serializable
data class ChapterResponse(
    val result: Result,
    val response: String,
    val data: Chapter
)



