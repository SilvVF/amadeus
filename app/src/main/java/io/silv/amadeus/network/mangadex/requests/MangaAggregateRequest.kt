package io.silv.amadeus.network.mangadex.requests

import io.silv.amadeus.network.mangadex.requests.query.QueryParams

data class MangaAggregateRequest(
    val translatedLanguage: List<String>? = null,
    val groups: List<String>? = null
): QueryParams