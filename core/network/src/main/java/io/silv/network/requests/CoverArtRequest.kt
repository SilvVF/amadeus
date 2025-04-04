package io.silv.network.requests

import io.silv.common.model.CoverIncludesFilter
import io.silv.common.model.Order
import io.silv.common.model.OrderBy
import io.silv.network.query.QueryParam
import io.silv.network.query.QueryParams
import io.silv.network.query.queryParamsOf

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
) : QueryParams {
    override fun createQueryParams(): List<QueryParam> {
        return queryParamsOf(
            Pair("limit", limit),
            Pair("offset", offset),
            Pair("manga", manga),
            Pair("ids", ids),
            Pair("uploaders", includes),
            Pair("locales", uploaders),
            Pair("order", order),
        )
    }
}
