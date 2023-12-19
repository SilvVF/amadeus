package io.silv.domain.manga

import io.silv.data.manga.MangaRepository

class MangaHandler(
    private val mangaRepository: MangaRepository,
) {
    suspend fun addOrRemoveFromLibrary(id: String) =
        runCatching {
            val manga = mangaRepository.getMangaById(id) ?: error("manga not found")

            mangaRepository.updateManga(
                manga.copy(
                    favorite = !manga.favorite
                )
            )
        }
}
