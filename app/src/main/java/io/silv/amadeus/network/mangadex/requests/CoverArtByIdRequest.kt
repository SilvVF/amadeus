package io.silv.amadeus.network.mangadex.requests

import io.silv.amadeus.network.mangadex.models.CoverIncludesFilter
import io.silv.amadeus.network.mangadex.requests.query.QueryParams

data class CoverArtByIdRequest(
    val includes: List<CoverIncludesFilter>? = null
): QueryParams