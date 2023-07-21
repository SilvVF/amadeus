package io.silv.manga.domain.usecase

import io.silv.manga.local.dao.PopularMangaResourceDao
import io.silv.manga.local.dao.RecentMangaResourceDao
import io.silv.manga.local.dao.SearchMangaResourceDao
import io.silv.manga.local.entity.MangaResource

internal fun interface GetMangaResourceById: suspend (String) -> Pair<MangaResource?, Int> {

    companion object {
        fun defaultImpl(
            recentMangaResourceDao: RecentMangaResourceDao,
            searchMangaResourceDao: SearchMangaResourceDao,
            popularMangaResourceDao: PopularMangaResourceDao,
        ) = GetMangaResourceById { id ->
            var daoUsed = -1
            val resource = recentMangaResourceDao.getMangaById(id).also { RecentMangaResourceDao.id } ?:
               searchMangaResourceDao.getMangaById(id).also { daoUsed = SearchMangaResourceDao.id}  ?:
               popularMangaResourceDao.getMangaById(id).also { daoUsed = PopularMangaResourceDao.id }
            resource to daoUsed
        }
    }
}