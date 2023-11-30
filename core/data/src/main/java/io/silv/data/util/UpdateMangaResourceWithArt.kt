package io.silv.data.util

import io.silv.database.dao.SourceMangaDao
import io.silv.database.entity.manga.MangaResource

internal class UpdateMangaResourceWithArt(
    private val sourceMangaDao: SourceMangaDao
) {

    suspend operator fun invoke(id: Int, mangaResource: MangaResource, volumeToCoverArt: Map<String, String>) {

        val prev = sourceMangaDao.selectById(mangaResource.id)

        if (prev != null) {
            sourceMangaDao.update(
                prev.copy(volumeToCoverArt = prev.volumeToCoverArt + volumeToCoverArt)
            )
        }
    }
}