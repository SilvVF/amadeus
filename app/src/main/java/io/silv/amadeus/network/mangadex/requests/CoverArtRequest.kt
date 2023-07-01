package io.silv.amadeus.network.mangadex.requests

import io.silv.amadeus.network.mangadex.models.CoverIncludesFilter
import io.silv.amadeus.network.mangadex.requests.query.QueryParams

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
): QueryParams