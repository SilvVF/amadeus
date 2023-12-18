package io.silv.domain.manga

import io.silv.data.manga.SavedMangaRepository
import io.silv.data.manga.SourceMangaRepository

class MangaHandler(
    private val savedRepository: SavedMangaRepository,
    private val sourceRepository: SourceMangaRepository,
) {
    suspend fun addOrRemoveFromLibrary(id: String) =
        runCatching {
            savedRepository.addOrRemoveFromLibrary(id)
        }
}
