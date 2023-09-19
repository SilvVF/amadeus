@file: Suppress("unused")

package io.silv.manga.network.mangadex.requests

import io.silv.manga.network.mangadex.requests.query.QueryParam
import io.silv.manga.network.mangadex.requests.query.QueryParams

data class MangaByIdRequest(
    val includes: List<String>? = null,
): QueryParams {
    override fun createQueryParams(): List<QueryParam> {
        return buildList {
            if (includes != null) {
                add(QueryParam("includes", includes.toString()))
            }
        }
    }
}