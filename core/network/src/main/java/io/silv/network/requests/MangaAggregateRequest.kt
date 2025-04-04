package io.silv.network.requests

import io.silv.network.query.QueryParam
import io.silv.network.query.QueryParams
import io.silv.network.query.queryParamsOf

data class MangaAggregateRequest(
    val translatedLanguage: List<String>? = null,
    val groups: List<String>? = null,
) : QueryParams {
    override fun createQueryParams(): List<QueryParam> {
        return queryParamsOf(
            Pair("translatedLanguage", translatedLanguage),
            Pair("groups", groups),
        )
    }
}
