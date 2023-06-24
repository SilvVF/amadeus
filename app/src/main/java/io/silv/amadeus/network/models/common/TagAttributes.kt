package io.silv.amadeus.network.models.common

import io.silv.amadeus.network.models.Group
import kotlinx.serialization.Serializable

@Serializable
data class TagAttributes(
    val name: LocalizedString,
    val description: LocalizedString,
    val group: Group,
    val version: Int,
    val relationships: List<Relationship> = emptyList()
)