package io.silv.amadeus.network.mangadex.models.cover

import io.silv.amadeus.network.mangadex.models.common.Relationship
import kotlinx.serialization.Serializable

@Serializable
data class CoverArtListResponse(
    val result: String,
    val response: String,
    val data: List<Cover>,
    val limit: Int,
    val offset: Int,
    val total: Int,
)


@Serializable
data class CoverArtByIdResponse(
    val result: String,
    val response: String,
    val data: List<Cover>
)

@Serializable
data class Cover(
    val id: String,
    val type: String,
    val attributes: CoverAttributes
)

@Serializable
data class CoverAttributes(
    val volume: String? = null,
    val fileName: String,
    val description: String? = null,
    val locale: String? = null,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
    val relationships: List<Relationship>
)