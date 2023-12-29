package io.silv.domain.manga.interactor

import io.silv.domain.manga.repository.MangaRepository

class MangaHandler(
    private val mangaRepository: MangaRepository,
) {
    suspend fun addOrRemoveFromLibrary(id: String) =
        runCatching {
            val manga = mangaRepository.getMangaById(id) ?: error("manga not found")

            mangaRepository.updateManga(
                manga.copy(
                    inLibrary = !manga.inLibrary
                )
            )
        }
}
