package io.silv.network.model.list

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Attributes(
    @SerialName("name")
    val name: String,
    @SerialName("version")
    val version: Int,
    @SerialName("visibility")
    val visibility: String,
)
