package io.silv.domain.manga

import io.silv.data.manga.SavedMangaRepository
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
class GetSavedMangaWithChaptersList(
    private val savedMangaRepository: SavedMangaRepository,
) {
    fun subscribe(): Flow<List<SavableMangaWithChapters>> {
        return  savedMangaRepository.observeSavedMangaListWithChapters().map { savedWithChapters ->
            savedWithChapters.map {
                SavableMangaWithChapters(
                    savableManga = SavableManga(it.manga),
                    chapters = it.chapters.map(::SavableChapter).toImmutableList()
                )
            }
        }
    }
}