package io.silv.amadeus.network.mangadex.models.manga

import io.silv.amadeus.network.mangadex.models.common.Relationship
import kotlinx.serialization.Serializable

@Serializable
data class Manga(
    val id: String,
    val type: String,
    val attributes: MangaAttributes,
    val relationships: List<Relationship> = emptyList()
)
