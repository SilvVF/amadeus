package io.silv.network.requests

import io.silv.network.requests.query.QueryParam
import io.silv.network.requests.query.QueryParams

data class MangaAggregateRequest(
    val translatedLanguage: List<String>? = null,
    val groups: List<String>? = null,
) : QueryParams {
    override fun createQueryParams(): List<QueryParam> {
        return listOf(
            Pair("translatedLanguage", translatedLanguage),
            Pair("groups", groups),
        )
            .filter { it.second != null }
            .map {
                QueryParam(it.first, it.second.toString())
            }
    }
}
