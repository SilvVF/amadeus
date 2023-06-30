package io.silv.amadeus.network.mangadex.models.chapter

import io.silv.amadeus.network.mangadex.models.common.Relationship
import kotlinx.serialization.Serializable

@Serializable
data class Chapter(
    val id: String,
    val type: String,
    val attributes: ChapterAttributes,
    val relationships: List<Relationship> = emptyList()
)