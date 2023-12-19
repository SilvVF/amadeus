package io.silv.data.manga

import io.silv.common.AmadeusDispatchers
import io.silv.data.util.Syncable
import io.silv.database.dao.MangaDao
import io.silv.database.entity.manga.MangaEntity
import io.silv.database.entity.relations.MangaEntityWithChapters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface MangaRepository: Syncable {

    suspend fun getMangaById(id: String): MangaEntity?

    suspend fun saveManga(manga: MangaEntity)

    suspend fun updateManga(manga: MangaEntity)

    suspend fun saveManga(list: List<MangaEntity>)

    fun observeMangaById(id: String): Flow<MangaEntity?>

    fun observeLibraryManga(): Flow<List<MangaEntity>>

    fun observeLibraryMangaWithChapters(): Flow<List<MangaEntityWithChapters>>
}

class MangaRepositoryImpl(
    private val mangaDao: MangaDao,
    private val dispatchers: AmadeusDispatchers,
): MangaRepository {

    override suspend fun getMangaById(id: String): MangaEntity? =
        withContext(dispatchers.io) {
            mangaDao.getById(id)
        }

    override suspend fun saveManga(manga: MangaEntity) =
        withContext(dispatchers.io) {
            mangaDao.insert(manga)
    }

    override suspend fun saveManga(list: List<MangaEntity>) =
        withContext(dispatchers.io) {
            mangaDao.insertAll(list)
    }

    override suspend fun updateManga(manga: MangaEntity) =
        withContext(dispatchers.io) {
            mangaDao.update(manga)
        }

    override fun observeMangaById(id: String): Flow<MangaEntity?> {
        return mangaDao.observeById(id)
    }

    override fun observeLibraryManga(): Flow<List<MangaEntity>> {
        return mangaDao.observeLibraryManga()
    }

    override fun observeLibraryMangaWithChapters(): Flow<List<MangaEntityWithChapters>> {
        return mangaDao.observeLibraryMangaWithChapters()
    }

    override suspend fun sync(): Boolean {
        return true
    }
}