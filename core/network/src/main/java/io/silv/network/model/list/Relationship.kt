package io.silv.network.model.list

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Relationship(
    @SerialName("id")
    val id: String,
    @SerialName("type")
    val type: String,
)
