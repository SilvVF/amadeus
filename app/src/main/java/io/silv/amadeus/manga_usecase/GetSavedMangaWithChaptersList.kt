package io.silv.amadeus.manga_usecase

import io.silv.amadeus.types.SavableManga
import io.silv.amadeus.types.SavableMangaWithChapters
import io.silv.manga.repositorys.manga.SavedMangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetSavedMangaWithChaptersList(
    private val savedMangaRepository: SavedMangaRepository,
    private val chapterInfoRepository: io.silv.manga.repositorys.chapter.ChapterEntityRepository,
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