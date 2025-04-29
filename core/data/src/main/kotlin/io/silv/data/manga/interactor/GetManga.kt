package io.silv.data.manga.interactor

import io.silv.data.manga.model.Manga
import io.silv.data.manga.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

class GetManga(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(id: String): Manga? {
        return mangaRepository.getMangaById(id)
    }

    fun subscribe(id: String): Flow<Manga> {
        return mangaRepository.observeMangaById(id).filterNotNull()
    }
}
