@file: Suppress("unused")

package io.silv.manga.network.mangadex.requests

import io.silv.manga.network.mangadex.requests.query.QueryParams

data class MangaByIdRequest(
    val includes: List<IncludesFilter>? = null,
): QueryParams {

    enum class IncludesFilter {
        manga, cover_art, author, artist, tag, creator
    }
}