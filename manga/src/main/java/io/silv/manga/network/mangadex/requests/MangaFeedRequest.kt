package io.silv.manga.network.mangadex.requests

import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.requests.query.QueryParams

/**
 * @param includes Available values : manga, scanlation_group, user
 * @param includeFutureUpdates Available values : 0, 1 Default value : 1
 * @param includeEmptyPages Available values : 0, 1
 * @param includeFuturePublishAt Available values : 0, 1
 * @param includeExternalUrl Available values : 0, 1
 */
data class MangaFeedRequest(
    val limit: Int = 100,
    val offset: Int = 0,
    val translatedLanguage: List<String>? = null,
    val originalLanguage: List<String>? = null,
    val excludedOriginalLanguage: List<String>? = null,
    val contentRating: List<ContentRating>? = null,
    val excludedGroups: List<String>? = null,
    val excludedUploaders: List<String>? = null,
    val includeFutureUpdates: String? = null,
    val createdAtSince: String? = null,
    val updatedAtSince: String? = null,
    val publishAtSince: String? = null,
    val order: Map<Order, OrderBy>? = null,
    val includes: List<String>? = null,
    val includeEmptyPages: Int? = null,
    val includeFuturePublishAt: Int? = null,
    val includeExternalUrl: Int? = null
): QueryParams
