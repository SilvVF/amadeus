package io.silv.network.requests

import io.silv.common.model.ContentRating
import io.silv.common.model.Order
import io.silv.common.model.OrderBy
import io.silv.network.query.QueryParam
import io.silv.network.query.QueryParams

/**
 * @param includes Available values : manga, scanlation_group, user
 * @param includeFutureUpdates Available values : 0, 1 Default value : 1
 * @param includeEmptyPages Available values : 0, 1
 * @param includeFuturePublishAt Available values : 0, 1
 * @param includeExternalUrl Available values : 0, 1
 */
data class MangaFeedRequest(
    val limit: Int? = 100,
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
    val includeExternalUrl: Int? = null,
) : QueryParams {
    override fun createQueryParams(): List<QueryParam> {
        return listOf(
            Pair("limit", limit),
            Pair("offset", offset),
            Pair("translatedLanguage", translatedLanguage),
            Pair("originalLanguage", originalLanguage),
            Pair("excludedOriginalLanguage", excludedOriginalLanguage),
            Pair("contentRating", contentRating),
            Pair("excludedGroups", excludedGroups),
            Pair("excludedUploaders", excludedUploaders),
            Pair("includeFutureUpdates", includeFutureUpdates),
            Pair("createdAtSince", createdAtSince),
            Pair("updatedAtSince", updatedAtSince),
            Pair("publishAtSince", publishAtSince),
            Pair("order", order),
            Pair("includes", includes),
            Pair("includeEmptyPages", includeEmptyPages),
            Pair("includeFuturePublishAt", includeFuturePublishAt),
            Pair("includeExternalUrl", includeExternalUrl),
        )
            .filter { it.second != null }
            .map {
                QueryParam(it.first, it.second.toString())
            }
    }
}
