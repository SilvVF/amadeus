package io.silv.manga.network.mangadex.requests

import io.silv.manga.network.mangadex.models.CoverIncludesFilter
import io.silv.manga.network.mangadex.requests.query.QueryParam
import io.silv.manga.network.mangadex.requests.query.QueryParams

/**
 * @param manga Manga ids (limited to 100 per request)
 * @param ids Covers ids (limited to 100 per request)
 * @param order { "createdAt": "asc","updatedAt": "asc","volume": "asc"}
* @param locales Locales of cover art (limited to 100 per request)
 * @param uploaders User ids (limited to 100 per request)
 */
data class CoverArtRequest(
    val limit: Int = 10,
    val offset: Int = 0,
    val manga: List<String>?,
    val ids: List<String>? = null,
    val includes: List<CoverIncludesFilter>? = null,
    val uploaders: List<String>? = null,
    val locales: List<String>? = null,
    val order: Map<Order, OrderBy>? = null,
): QueryParams {
    override fun createQueryParams(): List<QueryParam> {
        return listOf(
            Pair("limit", limit),
            Pair("offset", offset),
            Pair("manga", manga),
            Pair("ids", ids),
            Pair("uploaders", includes),
            Pair("locales", uploaders),
            Pair("order", order),
        )
            .filter { it.second != null }
            .map {
                QueryParam(it.first, it.second.toString())
            }
    }
}