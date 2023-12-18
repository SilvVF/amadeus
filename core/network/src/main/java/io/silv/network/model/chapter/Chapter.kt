package io.silv.network.model.chapter

import io.silv.network.model.common.Relationship
import kotlinx.serialization.Serializable

@Serializable
data class Chapter(
    val id: String,
    val type: String,
    val attributes: ChapterAttributes,
    val relationships: List<Relationship> = emptyList(),
)
