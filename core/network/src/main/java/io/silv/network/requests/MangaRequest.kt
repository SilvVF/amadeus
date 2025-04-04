package io.silv.network.requests

import io.silv.common.model.TagsMode
import io.silv.network.query.QueryParam
import io.silv.network.query.QueryParams
import io.silv.network.query.queryParamsOf

data class MangaRequest(
    val limit: Int = 10,
    val offset: Int = 0,
    val title: String? = null,
    val authorOrArtist: String? = null,
    val authors: List<String>? = null,
    val artists: List<String>? = null,
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
    val ids: List<String>? = null,
    val contentRating: List<String>? = null,
    var createdAtSince: String? = null,
    val updatedAtSince: String? = null,
    val order: Map<String, String>? = null,
    val includes: List<String>? = null,
    val hasAvailableChapters: Boolean? = null,
    val group: String? = null,
) : QueryParams {
    override fun createQueryParams(): List<QueryParam> {
        return queryParamsOf(
            Pair("limit", limit),
            Pair("offset", offset),
            Pair("title", title),
            Pair("authorOrArtist", authorOrArtist),
            Pair("authors", authors),
            Pair("artists", artists),
            Pair("year", year),
            Pair("includedTags", includedTags),
            Pair("includedTagsMode", includedTagsMode),
            Pair("excludedTags", excludedTags),
            Pair("excludedTagsMode", excludedTagsMode),
            Pair("status", status),
            Pair("originalLanguage", originalLanguage),
            Pair("excludedOriginalLanguage", excludedOriginalLanguage),
            Pair("availableTranslatedLanguage", availableTranslatedLanguage),
            Pair("publicationDemographic", publicationDemographic),
            Pair("ids", ids),
            Pair("contentRating", contentRating),
            Pair("createdAtSince", createdAtSince),
            Pair("updatedAtSince", updatedAtSince),
            Pair("order", order),
            Pair("includes", includes),
            Pair("hasAvailableChapters", hasAvailableChapters),
            Pair("group", group),
        )
    }
}
