package io.silv.manga.domain.usecase

import io.silv.core.combineTuple
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.FilteredMangaRepository
import io.silv.manga.domain.repositorys.FilteredYearlyMangaRepository
import io.silv.manga.domain.repositorys.PopularMangaRepository
import io.silv.manga.domain.repositorys.QuickSearchMangaRepository
import io.silv.manga.domain.repositorys.RecentMangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.SearchMangaRepository
import io.silv.manga.domain.repositorys.SeasonalMangaRepository
import io.silv.manga.domain.repositorys.chapter.ChapterEntityRepository
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.MangaResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

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
            SavableMangaWithChapters(
                savableManga = SavableManga(resources, saved),
                chapters = chapterInfo
            )
        }
    }
}

data class SavableMangaWithChapters(
    val savableManga: SavableManga?,
    val chapters: List<ChapterEntity>
)



class GetCombinedMangaResources(
    private val popularMangaRepository: PopularMangaRepository,
    private val recentMangaRepository: RecentMangaRepository,
    private val seasonalMangaRepository: SeasonalMangaRepository,
    private val searchMangaRepository: SearchMangaRepository,
    private val filteredMangaRepository: FilteredMangaRepository,
    private val filteredYearlyMangaRepository: FilteredYearlyMangaRepository,
    private val quickSearchMangaRepository: QuickSearchMangaRepository,
) {
    operator fun invoke(id: String): Flow<List<MangaResource>> {
        return combineTuple(
            popularMangaRepository.observeMangaResourceById(id),
            recentMangaRepository.observeMangaResourceById(id),
            seasonalMangaRepository.observeMangaResourceById(id),
            searchMangaRepository.observeMangaResourceById(id),
            filteredMangaRepository.observeMangaResourceById(id),
            filteredYearlyMangaRepository.observeMangaResourceById(id),
            quickSearchMangaRepository.observeMangaResourceById(id),
        ).map { (popular, recent, seasonal, search, filtered, yearly, quick) ->
            listOfNotNull(popular, recent, seasonal, search, filtered, yearly, quick)
        }
    }
}