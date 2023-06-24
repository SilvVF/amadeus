package io.silv.amadeus.network.mangadex.requests

import io.silv.amadeus.network.mangadex.models.CoverIncludesFilter
import io.silv.amadeus.network.mangadex.requests.util.QueryParams

data class CoverArtListRequest(
    val limit: Int = 10,
    val offset: Int = 0,
    val manga: List<String>? = null,
    val uploaders: List<String>? = null,
    val locales: List<String>? = null,
    val order: Map<String, String>? = null,
    val includes: List<CoverIncludesFilter>? = null
): QueryParams