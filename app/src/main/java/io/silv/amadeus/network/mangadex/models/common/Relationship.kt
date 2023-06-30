package io.silv.amadeus.network.mangadex.models.common

import io.silv.amadeus.network.mangadex.models.Related
import kotlinx.serialization.Serializable


@Serializable
data class Relationship(
    val id : String,
    val type: String,
    /**
     * Related Manga type, only present if you
     * are on a Manga entity and a Manga relationship
     */
    val related: Related? = null,
    /**
     *
    If Reference Expansion is applied, contains objects attributes
     */
    val attributes: Map<String, String?> = emptyMap()
)
