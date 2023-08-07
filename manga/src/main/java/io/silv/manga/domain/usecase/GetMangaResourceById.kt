package io.silv.manga.domain.usecase

import io.silv.manga.local.dao.FilteredMangaResourceDao
import io.silv.manga.local.dao.FilteredMangaYearlyResourceDao
import io.silv.manga.local.dao.PopularMangaResourceDao
import io.silv.manga.local.dao.QuickSearchMangaResourceDao
import io.silv.manga.local.dao.RecentMangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.dao.SearchMangaResourceDao
import io.silv.manga.local.dao.SeasonalMangaResourceDao
import io.silv.manga.local.entity.MangaResource
import kotlinx.coroutines.flow.firstOrNull

internal class GetMangaResourcesById(
    private val filteredMangaYearlyResourceDao: FilteredMangaYearlyResourceDao,
    private val filteredMangaResourceDao: FilteredMangaResourceDao,
    private val searchMangaResourceDao: SearchMangaResourceDao,
    private val seasonalMangaResourceDao: SeasonalMangaResourceDao,
    private val savedMangaResourceDao: SearchMangaResourceDao,
    private val popularMangaResourceDao: PopularMangaResourceDao,
    private val recentMangaResourceDao: RecentMangaResourceDao,
    private val quickSearchMangaResourceDao: QuickSearchMangaResourceDao,
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
            quickSearchMangaResourceDao.observeQuickSearchMangaResourceById(id).firstOrNull() to QuickSearchMangaResourceDao.id
        )
            .mapNotNull {
                (it.first ?: return@mapNotNull null) to it.second
            }
    }
}