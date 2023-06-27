package io.silv.amadeus.network.mangadex.models.common

import io.silv.amadeus.network.mangadex.models.Related
import kotlinx.serialization.Serializable


@Serializable
data class Relationship(
    val id : String,
    val type: String,
    val related: Related? = null,
    val attributes: Map<String, String?> = emptyMap()
)
