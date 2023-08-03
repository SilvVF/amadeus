package io.silv.manga.network.mangadex.requests

import io.silv.manga.network.mangadex.requests.query.QueryParams

data class ChapterListRequest(
    val ids: List<String>
): QueryParams