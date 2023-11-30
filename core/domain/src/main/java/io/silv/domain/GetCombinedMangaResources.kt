package io.silv.domain

import io.silv.database.dao.SourceMangaDao
import io.silv.database.entity.manga.SourceMangaResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * @property invoke Returns a Flow containing list of all the manga resources that have this id.
 * This is used for convenience to not have to do this in every flow that needs any resource.
 */
class GetCombinedMangaResources(
    private val sourceMangaDao: SourceMangaDao
) {
    operator fun invoke(id: String): Flow<List<SourceMangaResource>> {
        return sourceMangaDao.observeById(id).map { listOf(it) }
    }
}
