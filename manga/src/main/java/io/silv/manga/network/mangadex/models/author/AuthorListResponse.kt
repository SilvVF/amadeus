package io.silv.manga.network.mangadex.models.author

import io.silv.manga.network.mangadex.models.LocalizedString
import kotlinx.serialization.Serializable

@Serializable
data class AuthorListResponse(
    val result: String,
    val response: String,
    val data: List<Author>,
    val limit: Int,
    val offset: Int,
    val total: Int,
) {

    @Serializable
    data class Author(
        val id: String,
        val type: String,
        val attributes: AuthorAttributes,
        val relationships: List<Relationship>
    ) {

        @Serializable
        data class AuthorAttributes(
            val name: String,
            val imageUrl: String?= null,
            val biography: LocalizedString = emptyMap(),
            val twitter: String?= null,
            val pixiv: String?= null,
            val melonBook: String?= null,
            val booth: String?= null,
            val nicoVideo: String?= null,
            val skeb: String?= null,
            val fantia: String?= null,
            val tumblr: String?= null,
            val youtube: String?= null,
            val weibo: String?= null,
            val naver: String?= null,
            val website: String? = null,
            val version: Int,
            val createdAt: String,
            val updatedAt: String,
        )

        @Serializable
        data class Relationship(
            val id: String,
            val type: String,
            val related: String? = null,
            val attributes: Map<String, String>? = null
        )
    }
}