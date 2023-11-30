package io.silv.data.util

import io.silv.database.dao.SourceMangaDao
import io.silv.database.entity.manga.MangaResource

/**
 * Gets manga resources by given id and returns a list of the resource as well as the DAO id that it
 * came from.
 */
internal class GetMangaResourcesById(
    private val sourceDao: SourceMangaDao,
) {

    suspend operator fun invoke(id: String): List<Pair<MangaResource, Int>> {
        return listOf(
           sourceDao.selectById(id) to 0
        )
            .mapNotNull {
                (it.first ?: return@mapNotNull null) to it.second
            }
    }
}