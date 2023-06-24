package io.silv.amadeus.network.mangadex.models.chapter

import io.silv.amadeus.network.mangadex.models.Result
import io.silv.amadeus.network.mangadex.models.common.Relationship
import kotlinx.serialization.Serializable

@Serializable
data class ChapterListResponse(
    val result: String,
    val response: String,
    val data: List<Chapter>,
    val limit: Int,
    val offset: Int,
    val total: Int,
)

@Serializable
data class ChapterResponse(
    val result: Result,
    val response: String,
    val data: Chapter
)

@Serializable
data class Chapter(
    val id: String,
    val type: String,
    val attributes: ChapterAttributes,
    val relationships: List<Relationship>
)

@Serializable
data class ChapterAttributes(
    val title: String? = null,
    val volume: String? = null,
    val chapter: String? = null,
    val pages: Int,
    val translatedLanguage: String,
    val uploader: String,
    val externalUrl: String? = null,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
    val readableAt: String,
    val relationships: List<Relationship> = emptyList()
)