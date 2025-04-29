package io.silv.data.manga.interactor

import io.silv.common.model.ReadingStatus
import io.silv.data.manga.model.Manga
import io.silv.data.manga.model.toUpdate
import io.silv.data.manga.repository.MangaRepository
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

            mangaRepository.updateManga(newManga.toUpdate())

            newManga
        }

    suspend fun updateTrackedAfterTime(manga: Manga, time: LocalDateTime) = runCatching {
        mangaRepository.updateManga(
            manga.copy(
                savedAtLocal = time
            )
                .toUpdate()
        )
    }

    suspend fun updateMangaStatus(manga: Manga, readingStatus: ReadingStatus) = runCatching {
        mangaRepository.updateManga(
            manga.copy(readingStatus = readingStatus).toUpdate()
        )
    }
}
