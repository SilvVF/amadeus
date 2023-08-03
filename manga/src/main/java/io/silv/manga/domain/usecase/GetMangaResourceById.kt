package io.silv.manga.domain.usecase

import io.silv.manga.local.dao.FilteredMangaResourceDao
import io.silv.manga.local.dao.FilteredMangaYearlyResourceDao
import io.silv.manga.local.dao.PopularMangaResourceDao
import io.silv.manga.local.dao.RecentMangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.dao.SearchMangaResourceDao
import io.silv.manga.local.dao.SeasonalMangaResourceDao
import io.silv.manga.local.entity.MangaResource
import kotlinx.coroutines.flow.first

internal class GetMangaResourcesById(
    private val filteredMangaYearlyResourceDao: FilteredMangaYearlyResourceDao,
    private val filteredMangaResourceDao: FilteredMangaResourceDao,
    private val searchMangaResourceDao: SearchMangaResourceDao,
    private val seasonalMangaResourceDao: SeasonalMangaResourceDao,
    private val savedMangaResourceDao: SearchMangaResourceDao,
    private val popularMangaResourceDao: PopularMangaResourceDao,
    private val recentMangaResourceDao: RecentMangaResourceDao
) {

    suspend operator fun invoke(id: String): List<Pair<MangaResource, Int>> {
        return listOf(
            filteredMangaYearlyResourceDao.getFilteredMangaYearlyResourceById(id).first() to FilteredMangaYearlyResourceDao.id,
            filteredMangaResourceDao.getFilteredMangaResourcesById(id).first() to FilteredMangaResourceDao.id,
            searchMangaResourceDao.getResourceAsFlowById(id).first() to SearchMangaResourceDao.id,
            seasonalMangaResourceDao.getSeasonalMangaResourceById(id).first() to SeasonalMangaResourceDao.id,
            savedMangaResourceDao.getResourceAsFlowById(id).first() to SavedMangaDao.id,
            popularMangaResourceDao.getPopularMangaResourceById(id).first() to PopularMangaResourceDao.id,
            recentMangaResourceDao.getRecentMangaResourceById(id).first() to RecentMangaResourceDao.id
        )
            .mapNotNull {
                (it.first ?: return@mapNotNull null) to it.second
            }
    }
}