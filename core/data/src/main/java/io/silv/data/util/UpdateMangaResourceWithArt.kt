package io.silv.data.util

import android.util.Log
import io.silv.common.coroutine.suspendRunCatching
import io.silv.database.dao.FilteredMangaResourceDao
import io.silv.database.dao.FilteredMangaYearlyResourceDao
import io.silv.database.dao.PopularMangaResourceDao
import io.silv.database.dao.RecentMangaResourceDao
import io.silv.database.dao.SearchMangaResourceDao
import io.silv.database.dao.SeasonalMangaResourceDao
import io.silv.database.entity.manga.MangaResource
import io.silv.database.entity.manga.resource.FilteredMangaResource
import io.silv.database.entity.manga.resource.FilteredMangaYearlyResource
import io.silv.database.entity.manga.resource.PopularMangaResource
import io.silv.database.entity.manga.resource.RecentMangaResource
import io.silv.database.entity.manga.resource.SearchMangaResource
import io.silv.database.entity.manga.resource.SeasonalMangaResource

internal class UpdateMangaResourceWithArt(
    private val popularMangaResourceDao: PopularMangaResourceDao,
    private val recentMangaResourceDao: RecentMangaResourceDao,
    private val searchMangaResourceDao: SearchMangaResourceDao,
    private val filteredMangaResourceDao: FilteredMangaResourceDao,
    private val seasonalMangaResourceDao: SeasonalMangaResourceDao,
    private val filteredMangaYearlyResourceDao: FilteredMangaYearlyResourceDao,
) {

    suspend operator fun invoke(id: Int, mangaResource: MangaResource, volumeToCoverArt: Map<String, String>) {
        suspendRunCatching {
            when (id) {
               PopularMangaResourceDao.id -> {
                    popularMangaResourceDao.updatePopularMangaResource(
                        (mangaResource as PopularMangaResource).copy(
                            volumeToCoverArt = mangaResource.volumeToCoverArt + volumeToCoverArt
                        )
                    )
                }
                RecentMangaResourceDao.id -> {
                    recentMangaResourceDao.updateRecentMangaResource(
                        (mangaResource as RecentMangaResource).copy(
                            volumeToCoverArt = mangaResource.volumeToCoverArt + volumeToCoverArt
                        )
                    )
                }
               SearchMangaResourceDao.id -> {
                    searchMangaResourceDao.updateSearchMangaResource(
                        (mangaResource as SearchMangaResource).copy(
                            volumeToCoverArt = mangaResource.volumeToCoverArt + volumeToCoverArt
                        )
                    )
                }
                FilteredMangaResourceDao.id -> {
                    filteredMangaResourceDao.updateFilteredMangaResource(
                        (mangaResource as FilteredMangaResource).copy(
                            volumeToCoverArt = mangaResource.volumeToCoverArt + volumeToCoverArt
                        )
                    )
                }
                SeasonalMangaResourceDao.id -> {
                    seasonalMangaResourceDao.updateSeasonalMangaResource(
                        (mangaResource as SeasonalMangaResource).copy(
                            volumeToCoverArt = mangaResource.volumeToCoverArt + volumeToCoverArt
                        )
                    )
                }
                FilteredMangaYearlyResourceDao.id -> {
                    filteredMangaYearlyResourceDao.updateFilteredYearlyMangaResource(
                        (mangaResource as FilteredMangaYearlyResource).copy(
                            volumeToCoverArt = mangaResource.volumeToCoverArt + volumeToCoverArt
                        )
                    )
                }
            }
        }
            .onFailure {
                Log.d("cover_art", it.message ?: "err")
            }
    }
}