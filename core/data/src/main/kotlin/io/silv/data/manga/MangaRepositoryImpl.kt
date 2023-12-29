package io.silv.data.manga

import io.silv.common.AmadeusDispatchers
import io.silv.database.dao.MangaDao
import io.silv.domain.manga.model.Manga
import io.silv.domain.manga.model.MangaWithChapters
import io.silv.domain.manga.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MangaRepositoryImpl(
    private val mangaDao: MangaDao,
    private val dispatchers: AmadeusDispatchers,
): MangaRepository {

    override suspend fun getMangaById(id: String): Manga? =
        withContext(dispatchers.io) {
            mangaDao.getById(id)?.let(MangaMapper::mapManga)
        }

    override suspend fun saveManga(manga: Manga) =
        withContext(dispatchers.io) {
            mangaDao.insert(manga.let(MangaMapper::toEntity))
    }

    override suspend fun saveManga(list: List<Manga>) =
        withContext(dispatchers.io) {
            mangaDao.insertAll(list.map(MangaMapper::toEntity))
    }

    override suspend fun updateManga(manga: Manga) =
        withContext(dispatchers.io) {
            mangaDao.update(manga.let(MangaMapper::toEntity))
        }

    override fun observeMangaById(id: String): Flow<Manga?> {
        return mangaDao.observeById(id).map { manga -> manga?.let(MangaMapper::mapManga) }
    }

    override fun observeManga(): Flow<List<Manga>> {
        return mangaDao.observeAll().map { list -> list.map(MangaMapper::mapManga) }
    }

    override fun observeLibraryManga(): Flow<List<Manga>> {
        return mangaDao.observeLibraryManga().map { list -> list.map(MangaMapper::mapManga) }
    }

    override fun observeLibraryMangaWithChapters(): Flow<List<MangaWithChapters>> {
        return mangaDao.observeLibraryMangaWithChapters().map {
            it.map(MangaMapper::mapMangaWithChapters)
        }
    }

    override suspend fun sync(): Boolean {
        return true
    }
}