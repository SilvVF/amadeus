package io.silv.network.util

import com.skydoves.sandwich.getOrThrow
import io.silv.common.pmap
import io.silv.network.MangaDexApi
import io.silv.network.model.manga.MangaDto
import io.silv.network.requests.MangaRequest

@Throws(RuntimeException::class)
suspend fun MangaDexApi.fetchMangaChunked(
    ids: List<String>,
    chunkSize: Int = 100,
    request: MangaRequest = MangaRequest(),
): List<MangaDto> {
    return ids.chunked(chunkSize)
        .pmap {
            getMangaList(
                request.copy(
                    limit = 100,
                    ids = it,
                    includes = listOf("cover_art", "author", "artist"),
                    order = mapOf("followedCount" to "desc"),
                    hasAvailableChapters = true
                )
            )
                .getOrThrow()
                .data
        }
        .flatten()
}