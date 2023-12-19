package io.silv.domain.manga

import io.silv.data.manga.MangaRepository
import io.silv.model.SavableChapter
import io.silv.model.SavableManga
import io.silv.model.SavableMangaWithChapters
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Gets all saved mangas and combines them with the Chapter Entity's
 * that have the the manga id as their foreign key.
 */
class GetLibraryMangaWithChapters(
    private val mangaRepository: MangaRepository,
) {
    fun subscribe(): Flow<List<SavableMangaWithChapters>> {
        return mangaRepository.observeLibraryMangaWithChapters().map { mangaWithChapters ->
            mangaWithChapters.map {
                SavableMangaWithChapters(
                    savableManga = SavableManga(it.manga),
                    chapters = it.chapters.map(::SavableChapter).toImmutableList(),
                )
            }
        }
    }
}
