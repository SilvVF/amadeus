package io.silv.manga.domain.usecase

import io.silv.manga.local.dao.FilteredMangaResourceDao
import io.silv.manga.local.dao.FilteredMangaYearlyResourceDao
import io.silv.manga.local.dao.PopularMangaResourceDao
import io.silv.manga.local.dao.RecentMangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.dao.SearchMangaResourceDao
import io.silv.manga.local.dao.SeasonalMangaResourceDao
import io.silv.manga.local.entity.MangaResource

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
            filteredMangaYearlyResourceDao.getMangaById(id) to FilteredMangaYearlyResourceDao.id,
            filteredMangaResourceDao.getMangaById(id) to FilteredMangaResourceDao.id,
            searchMangaResourceDao.getMangaById(id) to SearchMangaResourceDao.id,
            seasonalMangaResourceDao.getMangaById(id) to SeasonalMangaResourceDao.id,
            savedMangaResourceDao.getMangaById(id) to SavedMangaDao.id,
            popularMangaResourceDao.getMangaById(id) to PopularMangaResourceDao.id,
            recentMangaResourceDao.getMangaById(id) to RecentMangaResourceDao.id
        )
            .mapNotNull {
                (it.first ?: return@mapNotNull null) to it.second
            }
    }
}