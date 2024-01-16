package io.silv.domain.manga.interactor

import io.silv.domain.manga.model.Manga
import io.silv.domain.manga.repository.MangaRepository
import kotlinx.datetime.LocalDateTime

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

    suspend fun updateTrackedAfterTime(manga: Manga, time: LocalDateTime) = runCatching {
        mangaRepository.updateManga(
            manga.copy(
                savedAtLocal = time
            )
        )
    }
}
