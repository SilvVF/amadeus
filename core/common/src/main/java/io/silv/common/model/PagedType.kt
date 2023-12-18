package io.silv.common.model

import kotlinx.datetime.LocalDateTime

sealed class PagedType {
    data object Popular : PagedType()

    data object Latest : PagedType()

    data class TimePeriod(val tagId: String, val timePeriod: io.silv.common.model.TimePeriod) : PagedType()

    data class Query(
        val filters: QueryFilters = QueryFilters(),
    ) : PagedType()
}

data class QueryFilters(
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
    var createdAtSince: LocalDateTime? = null,
    val updatedAtSince: LocalDateTime? = null,
    val order: Map<String, String>? = null,
    val includes: List<String>? = null,
    val hasAvailableChapters: Boolean? = null,
    val group: String? = null,
)
