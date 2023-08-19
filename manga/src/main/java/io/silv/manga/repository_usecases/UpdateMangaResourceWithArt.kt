package io.silv.manga.repository_usecases

import android.util.Log
import io.silv.manga.local.dao.FilteredMangaResourceDao
import io.silv.manga.local.dao.FilteredMangaYearlyResourceDao
import io.silv.manga.local.dao.PopularMangaResourceDao
import io.silv.manga.local.dao.RecentMangaResourceDao
import io.silv.manga.local.dao.SearchMangaResourceDao
import io.silv.manga.local.dao.SeasonalMangaResourceDao
import io.silv.manga.local.entity.MangaResource
import io.silv.manga.local.entity.manga_resource.FilteredMangaResource
import io.silv.manga.local.entity.manga_resource.FilteredMangaYearlyResource
import io.silv.manga.local.entity.manga_resource.PopularMangaResource
import io.silv.manga.local.entity.manga_resource.RecentMangaResource
import io.silv.manga.local.entity.manga_resource.SearchMangaResource
import io.silv.manga.local.entity.manga_resource.SeasonalMangaResource
import io.silv.manga.repositorys.suspendRunCatching

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