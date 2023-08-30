package io.silv.amadeus.manga_usecase

import com.zhuinden.flowcombinetuplekt.combineTuple
import io.silv.manga.local.entity.MangaResource
import io.silv.manga.repositorys.manga.FilteredMangaRepository
import io.silv.manga.repositorys.manga.FilteredYearlyMangaRepository
import io.silv.manga.repositorys.manga.PopularMangaRepository
import io.silv.manga.repositorys.manga.QuickSearchMangaRepository
import io.silv.manga.repositorys.manga.RecentMangaRepository
import io.silv.manga.repositorys.manga.SearchMangaRepository
import io.silv.manga.repositorys.manga.SeasonalMangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * @property invoke Returns a Flow containing list of all the manga resources that have this id.
 * This is used for convenience to not have to do this in every flow that needs any resource.
 */
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
