package io.silv.amadeus.network.mangadex.requests

import io.silv.amadeus.network.mangadex.models.CoverIncludesFilter
import io.silv.amadeus.network.mangadex.requests.util.QueryParams

data class CoverArtByIdRequest(
    val includes: List<CoverIncludesFilter>? = null
): QueryParams