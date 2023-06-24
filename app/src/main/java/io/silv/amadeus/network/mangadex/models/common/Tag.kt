package io.silv.amadeus.network.mangadex.models.common


import kotlinx.serialization.Serializable


@Serializable
data class Tag(
    val id: String,
    val type: String,
    val attributes: TagAttributes,
    val relationships: List<Relationship> = emptyList()
)