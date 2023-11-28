package io.silv.network.requests

import io.silv.network.requests.query.QueryParam
import io.silv.network.requests.query.QueryParams

data class ChapterListRequest(
    val ids: List<String>
): QueryParams {

    override fun createQueryParams(): List<QueryParam> {
        return listOf(
            QueryParam("ids", ids.toString())
        )
    }
}