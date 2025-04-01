package io.silv.domain.history

import io.silv.domain.manga.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

class GetLibraryLastUpdated(
    private val mangaRepository: MangaRepository
) {

    fun subscribe(): Flow<LocalDateTime?> {
        return mangaRepository.observeLastLibrarySynced()
    }
}