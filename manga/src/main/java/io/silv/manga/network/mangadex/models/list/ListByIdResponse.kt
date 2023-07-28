package io.silv.manga.network.mangadex.models.list


import io.silv.manga.network.mangadex.models.Related
import io.silv.manga.network.mangadex.models.Result
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListByIdResponse(

    @SerialName("result")
    val result: Result,
    val data: CustomList
) {

    @Serializable
    data class CustomList(
        val id : String,
        val type: String,
        val attributes: CustomListAttributes,
        val relationships: List<ListRelationship> = emptyList()
    ) {

        @Serializable
        data class CustomListAttributes(
            val name: String,
            val visibility: String,
            val version: Int,
        )

        @Serializable
        data class ListRelationship(
            val id: String,
            val type: String,
            val related: Related? = null,
            val attributes: List<Attributes>? = emptyList()
        ) {
            @Serializable
            data class Attributes(
                val description: String
            )
        }
    }
}