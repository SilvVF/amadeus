package io.silv.data.manga.interactor

import io.silv.common.log.logcat
import io.silv.common.model.ReadingStatus
import io.silv.data.download.CoverCache
import io.silv.data.manga.model.Manga
import io.silv.data.manga.model.toResource
import io.silv.data.manga.model.toUpdate
import io.silv.data.manga.repository.MangaRepository
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

class MangaHandler(
    private val mangaRepository: MangaRepository,
    private val coverCache: CoverCache,
) {
    suspend fun addOrRemoveFromLibrary(id: String) =
        runCatching<Manga> {
            logcat { "adding or removing from library" }
            val manga = mangaRepository.getMangaById(id) ?: error("manga not found")

            val newManga = manga.copy(
                inLibrary = !manga.inLibrary
            )

            mangaRepository.updateManga(newManga.toUpdate())

            newManga
        }
            .onFailure {
                logcat { it.stackTraceToString() }
            }
            .onSuccess {
                if (!it.inLibrary) {
                    coverCache.deleteFromCache(
                        it.toResource(),
                        true
                    )
                }
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
