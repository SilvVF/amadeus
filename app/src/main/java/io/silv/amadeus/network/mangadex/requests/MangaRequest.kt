@file:Suppress("unused")

package io.silv.amadeus.network.mangadex.requests

import io.silv.amadeus.network.mangadex.requests.query.QueryParams


data class MangaRequest(
    val limit: Int = 10,
    val offset: Int = 0,
    val title: String?  = null,
    val authorOrArtist: String? = null,
    val authors: List<String>? = null,
    val year: Int? = null,
    val includedTags: List<String>? = null,
    val includedTagsMode: TagsMode? = null,
    val excludedTags: List<String>? = null,
    val excludedTagsMode: TagsMode? = null,
    val status: List<String>? = null,
    val originalLanguage: List<String>? = null,
    val excludedOriginalLanguage: List<String>? = null,
    val availableTranslatedLanguage: List<String>? = null,
    val publicationDemographic: List<String>? = null,
    val ids: List<String>?  = null,
    val contentRating: List<String>?  = null,
    val createdAtSince: String? = null,
    val updatedAtSince: String?  = null,
    val order: Map<Order, OrderBy>? = null,
    val includes: List<String>? = null,
    val hasAvailableChapter: Boolean? = null,
    val group: String? = null
): QueryParams {


    enum class TagsMode(
        val string: String
    ) {
        OR("OR"), AND("AND")
    }
}

