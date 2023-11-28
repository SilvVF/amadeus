package io.silv.domain

import com.zhuinden.flowcombinetuplekt.combineTuple
import io.silv.data.manga.FilteredMangaRepository
import io.silv.data.manga.FilteredYearlyMangaRepository
import io.silv.data.manga.PopularMangaRepository
import io.silv.data.manga.QuickSearchMangaRepository
import io.silv.data.manga.RecentMangaRepository
import io.silv.data.manga.SearchMangaRepository
import io.silv.data.manga.SeasonalMangaRepository
import io.silv.database.entity.manga.MangaResource
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
