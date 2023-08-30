package io.silv.manga.repository_usecases

import io.silv.manga.local.dao.FilteredMangaResourceDao
import io.silv.manga.local.dao.FilteredMangaYearlyResourceDao
import io.silv.manga.local.dao.PopularMangaResourceDao
import io.silv.manga.local.dao.QuickSearchMangaResourceDao
import io.silv.manga.local.dao.RecentMangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.dao.SearchMangaResourceDao
import io.silv.manga.local.dao.SeasonalMangaResourceDao
import io.silv.manga.local.dao.TempMangaResourceDao
import io.silv.manga.local.entity.MangaResource
import kotlinx.coroutines.flow.firstOrNull

/**
 * Gets manga resources by given id and returns a list of the resource as well as the DAO id that it
 * came from.
 */
internal class GetMangaResourcesById(
    private val filteredMangaYearlyResourceDao: FilteredMangaYearlyResourceDao,
    private val filteredMangaResourceDao: FilteredMangaResourceDao,
    private val searchMangaResourceDao: SearchMangaResourceDao,
    private val seasonalMangaResourceDao: SeasonalMangaResourceDao,
    private val savedMangaResourceDao: SearchMangaResourceDao,
    private val popularMangaResourceDao: PopularMangaResourceDao,
    private val recentMangaResourceDao: RecentMangaResourceDao,
    private val quickSearchMangaResourceDao: QuickSearchMangaResourceDao,
    private val tempMangaResourceDao: TempMangaResourceDao,
) {

    suspend operator fun invoke(id: String): List<Pair<MangaResource, Int>> {
        return listOf(
            filteredMangaYearlyResourceDao.observeFilteredYearlyMangaResourceById(id).firstOrNull() to FilteredMangaYearlyResourceDao.id,
            filteredMangaResourceDao.observeFilteredMangaResourceById(id).firstOrNull() to FilteredMangaResourceDao.id,
            searchMangaResourceDao.observeSearchMangaResourceById(id).firstOrNull() to SearchMangaResourceDao.id,
            seasonalMangaResourceDao.observeSeasonalMangaResourceById(id).firstOrNull() to SeasonalMangaResourceDao.id,
            savedMangaResourceDao.observeSearchMangaResourceById(id).firstOrNull() to SavedMangaDao.id,
            popularMangaResourceDao.observePopularMangaResourceById(id).firstOrNull() to PopularMangaResourceDao.id,
            recentMangaResourceDao.observeRecentMangaResourceById(id).firstOrNull() to RecentMangaResourceDao.id,
            quickSearchMangaResourceDao.observeQuickSearchMangaResourceById(id).firstOrNull() to QuickSearchMangaResourceDao.id,
            tempMangaResourceDao.observeTempMangaResourceById(id).firstOrNull() to TempMangaResourceDao.id
        )
            .mapNotNull {
                (it.first ?: return@mapNotNull null) to it.second
            }
    }
}