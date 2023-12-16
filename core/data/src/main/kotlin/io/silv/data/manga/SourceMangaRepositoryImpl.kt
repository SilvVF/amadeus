package io.silv.data.manga

import io.silv.common.AmadeusDispatchers
import io.silv.database.dao.SourceMangaDao
import io.silv.database.entity.manga.SourceMangaResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface SourceMangaRepository {

    suspend fun saveManga(manga: SourceMangaResource)

    suspend fun saveManga(list: List<SourceMangaResource>)

    fun observeMangaById(id: String): Flow<SourceMangaResource?>
}

class SourceMangaRepositoryImpl(
    private val sourceMangaDao: SourceMangaDao,
    private val dispatchers: AmadeusDispatchers,
): SourceMangaRepository {

    override suspend fun saveManga(manga: SourceMangaResource) =
        withContext(dispatchers.io) {
            sourceMangaDao.insert(manga)
    }

    override suspend fun saveManga(list: List<SourceMangaResource>) =
        withContext(dispatchers.io) {
            sourceMangaDao.insertAll(list)
    }

    override fun observeMangaById(id: String): Flow<SourceMangaResource?> {
        return sourceMangaDao.observeById(id)
    }
}