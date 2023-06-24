package io.silv.amadeus.network.mangadex.requests

import io.silv.amadeus.network.mangadex.requests.util.QueryParams

data class MangaAggregateRequest(
    val translatedLanguage: List<String>? = null,
    val groups: List<String>? = null
): QueryParams