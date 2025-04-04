package io.silv.network.requests

import io.silv.network.query.QueryParam
import io.silv.network.query.QueryParams
import io.silv.network.query.queryParamsOf

data class ChapterListRequest(
    val ids: List<String>,
) : QueryParams {
    override fun createQueryParams(): List<QueryParam> {
        return queryParamsOf(
            Pair("ids", ids)
        )
    }
}
