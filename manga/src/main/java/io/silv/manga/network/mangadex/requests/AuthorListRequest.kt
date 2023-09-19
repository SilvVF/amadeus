package io.silv.manga.network.mangadex.requests

import io.silv.manga.network.mangadex.requests.query.QueryParam
import io.silv.manga.network.mangadex.requests.query.QueryParams

data class AuthorListRequest(
    val limit: Int? = null,
    val offset: Int = 0,
    val ids: List<String>? = null,
    val name: String? = null,
    val order: Map<String, String>? = null
): QueryParams {

    override fun createQueryParams(): List<QueryParam> {
        return listOf(
            Pair("limit", limit),
            Pair("offset", offset),
            Pair("ids", ids),
            Pair("name", name),
            Pair("order", order),
        )
            .filter { it.second != null }
            .map {
                QueryParam(it.first, it.second.toString())
            }
    }
}