package io.silv.manga.network.mangadex.models.list


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Data(
    @SerialName("attributes")
    val attributes: Attributes,
    @SerialName("id")
    val id: String,
    @SerialName("relationships")
    val relationships: List<Relationship>,
    @SerialName("type")
    val type: String
)