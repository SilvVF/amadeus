package io.silv.network.requests

import io.silv.network.query.QueryParam
import io.silv.network.query.QueryParams
import io.silv.network.query.queryParamsOf

data class AuthorListRequest(
    val limit: Int? = null,
    val offset: Int = 0,
    val ids: List<String>? = null,
    val name: String? = null,
    val order: Map<String, String>? = null,
) : QueryParams {

    override fun createQueryParams(): List<QueryParam> {
        return queryParamsOf(
            Pair("limit", limit),
            Pair("offset", offset),
            Pair("ids", ids),
            Pair("name", name),
            Pair("order", order),
        )
    }
}
