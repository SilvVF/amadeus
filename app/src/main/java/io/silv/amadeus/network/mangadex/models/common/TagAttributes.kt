package io.silv.amadeus.network.mangadex.models.common

import io.silv.amadeus.network.mangadex.models.Group
import io.silv.amadeus.network.mangadex.models.LocalizedString
import kotlinx.serialization.Serializable

@Serializable
data class TagAttributes(
    val name: LocalizedString,
    val description: LocalizedString,
    val group: Group,
    val version: Int,
    val relationships: List<Relationship> = emptyList()
)