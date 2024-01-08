package io.silv.domain.manga.interactor

import io.silv.domain.manga.model.Manga
import io.silv.domain.manga.repository.MangaRepository

class MangaHandler(
    private val mangaRepository: MangaRepository,
) {
    suspend fun addOrRemoveFromLibrary(id: String) =
        runCatching<Manga> {
            val manga = mangaRepository.getMangaById(id) ?: error("manga not found")

            val newManga = manga.copy(
                inLibrary = !manga.inLibrary
            )

            mangaRepository.updateManga(newManga)

            newManga
        }
}
