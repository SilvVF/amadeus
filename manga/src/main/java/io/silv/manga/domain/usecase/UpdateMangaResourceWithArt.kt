package io.silv.manga.domain.usecase

import android.util.Log
import io.silv.manga.domain.suspendRunCatching
import io.silv.manga.local.dao.FilteredMangaResourceDao
import io.silv.manga.local.dao.FilteredMangaYearlyResourceDao
import io.silv.manga.local.dao.PopularMangaResourceDao
import io.silv.manga.local.dao.RecentMangaResourceDao
import io.silv.manga.local.dao.SearchMangaResourceDao
import io.silv.manga.local.dao.SeasonalMangaResourceDao
import io.silv.manga.local.entity.FilteredMangaResource
import io.silv.manga.local.entity.FilteredMangaYearlyResource
import io.silv.manga.local.entity.MangaResource
import io.silv.manga.local.entity.PopularMangaResource
import io.silv.manga.local.entity.RecentMangaResource
import io.silv.manga.local.entity.SearchMangaResource
import io.silv.manga.local.entity.SeasonalMangaResource

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
                    popularMangaResourceDao.update(
                        (mangaResource as PopularMangaResource).copy(
                            volumeToCoverArt = mangaResource.volumeToCoverArt + volumeToCoverArt
                        )
                    )
                }

                RecentMangaResourceDao.id -> {
                    recentMangaResourceDao.update(
                        (mangaResource as RecentMangaResource).copy(
                            volumeToCoverArt = mangaResource.volumeToCoverArt + volumeToCoverArt
                        )
                    )
                }

                SearchMangaResourceDao.id -> {
                    searchMangaResourceDao.update(
                        (mangaResource as SearchMangaResource).copy(
                            volumeToCoverArt = mangaResource.volumeToCoverArt + volumeToCoverArt
                        )
                    )
                }

                FilteredMangaResourceDao.id -> {
                    filteredMangaResourceDao.update(
                        (mangaResource as FilteredMangaResource).copy(
                            volumeToCoverArt = mangaResource.volumeToCoverArt + volumeToCoverArt
                        )
                    )
                }

                SeasonalMangaResourceDao.id -> {
                    seasonalMangaResourceDao.update(
                        (mangaResource as SeasonalMangaResource).copy(
                            volumeToCoverArt = mangaResource.volumeToCoverArt + volumeToCoverArt
                        )
                    )
                }

                FilteredMangaYearlyResourceDao.id -> {
                    filteredMangaYearlyResourceDao.update(
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