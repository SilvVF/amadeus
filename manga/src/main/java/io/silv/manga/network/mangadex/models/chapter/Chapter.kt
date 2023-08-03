package io.silv.manga.network.mangadex.models.chapter

import io.silv.manga.network.mangadex.models.common.Relationship
import kotlinx.serialization.Serializable

@Serializable
data class Chapter(
    val id: String,
    val type: String,
    val attributes: ChapterAttributes,
    val relationships: List<Relationship> = emptyList()
)
