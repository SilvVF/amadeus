package io.silv.manga.network.mangadex.models.tags


import io.silv.manga.network.mangadex.models.LocalizedString
import kotlinx.serialization.Serializable

@Serializable
data class TagResponse(
    val result: String,
    val response: String,
    val data: List<Tag>,
    val limit: Int,
    val offset: Int,
    val total: Int
) {

    @Serializable
    data class Tag(
        val id: String,
        val type: String,
        val attributes: TagAttributes,
        val relationships: List<Relationship>
    ) {
        @Serializable
        data class TagAttributes(
            val name: LocalizedString,
            val description: LocalizedString,
            val group: String,
            val version: Int,
        )
    }

    @Serializable
    data class Relationship(
        val id: String,
        val type: String,
        val related: String,
        val attributes: Map<String, String>? = null,
    )
}