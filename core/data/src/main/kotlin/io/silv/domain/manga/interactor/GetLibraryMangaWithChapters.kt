package io.silv.domain.manga.interactor

import io.silv.domain.manga.model.MangaWithChapters
import io.silv.domain.manga.repository.MangaRepository
import kotlinx.coroutines.flow.Flow

/**
 * Gets all saved mangas and combines them with the Chapter Entity's
 * that have the the manga id as their foreign key.
 */
class GetLibraryMangaWithChapters(
    private val mangaRepository: MangaRepository,
) {
    fun subscribe(): Flow<List<MangaWithChapters>> {
        return mangaRepository.observeLibraryMangaWithChapters()
    }

    suspend fun await(): List<MangaWithChapters> {
        return mangaRepository.getLibraryMangaWithChapters()
    }
}
