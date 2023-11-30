package io.silv.domain

import io.silv.data.chapter.ChapterEntityRepository
import io.silv.data.manga.SavedMangaRepository
import io.silv.model.SavableManga
import io.silv.model.SavableMangaWithChapters
import io.silv.toSavable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Combines Saved manga with all resource repository's and transforms the manga received by id into a
 * flow of [SavableMangaWithChapters]. This contains all chapters with this mangaId as a foreign key.
 * If the manga is not found it will try to fetch it from the MangaDexApi and save it in memory.
 */
class GetCombinedSavableMangaWithChapters(
    private val getCombinedMangaResources: GetCombinedMangaResources,
    private val savedMangaRepository: SavedMangaRepository,
    private val chapterInfoRepository: ChapterEntityRepository,
) {

    operator fun invoke(id: String): Flow<SavableMangaWithChapters> {
        return combine(
            getCombinedMangaResources(id),
            savedMangaRepository.getSavedManga(id),
            chapterInfoRepository.getChapters(id),
        ) { resources, saved, chapterInfo ->

            return@combine saved?.let { savedManga ->
                SavableMangaWithChapters(
                    savableManga = savedManga.toSavable(resources.ifEmpty { null }, savedManga),
                    chapters = chapterInfo
                )
            } ?: SavableMangaWithChapters(
                savableManga = SavableManga(resources.first(), null),
                chapters = chapterInfo
            )
        }
    }
}






