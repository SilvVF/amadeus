package io.silv.network.model.chapter

import io.silv.network.model.common.Relationship
import kotlinx.serialization.Serializable

@Serializable
data class ChapterAttributes(
    val title: String? = null,
    val volume: String? = null,
    val chapter: String? = null,
    /**
     * Count of readable images for this chapter
     */
    val pages: Int,
    val translatedLanguage: String? = null,
    val uploader: String? = null,
    val externalUrl: String? = null,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
    val readableAt: String,
    val relationships: List<Relationship> = emptyList()
)