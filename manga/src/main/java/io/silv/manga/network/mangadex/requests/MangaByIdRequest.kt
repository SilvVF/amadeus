@file: Suppress("unused")

package io.silv.manga.network.mangadex.requests

import io.silv.manga.network.mangadex.requests.query.QueryParams

data class MangaByIdRequest(
    val includes: List<String>? = null,
): QueryParams