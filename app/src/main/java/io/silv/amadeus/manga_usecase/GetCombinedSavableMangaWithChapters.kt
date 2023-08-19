package io.silv.amadeus.manga_usecase

import io.silv.amadeus.types.SavableManga
import io.silv.amadeus.types.SavableMangaWithChapters
import io.silv.amadeus.types.toSavable
import io.silv.manga.repository_usecases.GetCombinedMangaResources
import io.silv.manga.repositorys.manga.SavedMangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine


class GetCombinedSavableMangaWithChapters(
    private val getCombinedMangaResources: GetCombinedMangaResources,
    private val savedMangaRepository: SavedMangaRepository,
    private val chapterInfoRepository: io.silv.manga.repositorys.chapter.ChapterEntityRepository,
) {
    operator fun invoke(id: String): Flow<SavableMangaWithChapters> {
        return combine(
            getCombinedMangaResources(id),
            savedMangaRepository.getSavedManga(id),
            chapterInfoRepository.getChapters(id),
        ) { resources, saved, chapterInfo ->
            return@combine saved?.let {
                SavableMangaWithChapters(
                    savableManga = it.toSavable(resources.ifEmpty { null }, saved),
                    chapters = chapterInfo
                )
            } ?: SavableMangaWithChapters(
                savableManga = SavableManga(resources, saved),
                chapters = chapterInfo
            )
        }
    }
}






