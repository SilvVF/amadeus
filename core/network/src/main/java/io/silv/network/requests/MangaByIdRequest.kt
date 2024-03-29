package io.silv.network.requests

import io.silv.network.query.QueryParam
import io.silv.network.query.QueryParams

data class MangaByIdRequest(
    val includes: List<String>? = null,
) : QueryParams {
    override fun createQueryParams(): List<QueryParam> {
        return buildList {
            if (includes != null) {
                add(QueryParam("includes", includes.toString()))
            }
        }
    }
}
