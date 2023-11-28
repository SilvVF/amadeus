package io.silv.amadeus.manga_usecase

import io.silv.amadeus.types.SavableManga
import io.silv.amadeus.types.SavableMangaWithChapters
import io.silv.data.chapter.ChapterEntityRepository
import io.silv.data.manga.SavedMangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Gets all saved mangas and combines them with the Chapter Entity's
 * that have the the manga id as their foreign key.
 */
class GetSavedMangaWithChaptersList(
    private val savedMangaRepository: SavedMangaRepository,
    private val chapterInfoRepository: ChapterEntityRepository,
) {
    operator fun invoke(): Flow<List<SavableMangaWithChapters>> {
        return combine(
            savedMangaRepository.getSavedMangas(),
            chapterInfoRepository.getAllChapters(),
        ) { saved, chapterInfo ->
            saved.map { entity ->
                SavableMangaWithChapters(
                    savableManga = SavableManga(entity),
                    chapters = chapterInfo.filter { it.mangaId == entity.id }
                )
            }
        }
    }
}