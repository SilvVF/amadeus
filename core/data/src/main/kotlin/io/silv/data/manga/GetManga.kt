package io.silv.data.manga

import io.silv.common.model.MangaResource
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull

internal class GetManga(
    private val sourceMangaRepository: SourceMangaRepository,
    private val savedMangaRepository: SavedMangaRepository,
) {

    suspend fun await(id: String): MangaResource? {
        return combine(
            sourceMangaRepository.observeMangaById(id),
            savedMangaRepository.observeSavedMangaById(id)
        ) { source, saved ->
            saved ?: source
        }
            .firstOrNull()
    }
}