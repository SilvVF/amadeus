package io.silv.network.model.list


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserIdListResponse(
    @SerialName("data")
    val data: List<Data>,
    @SerialName("limit")
    val limit: Int,
    @SerialName("offset")
    val offset: Int,
    @SerialName("response")
    val response: String,
    @SerialName("result")
    val result: String,
    @SerialName("total")
    val total: Int
)