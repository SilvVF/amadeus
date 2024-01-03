package io.silv.data.manga

import androidx.room.withTransaction
import io.silv.common.AmadeusDispatchers
import io.silv.data.util.CoverCache
import io.silv.data.util.deleteOldCoverFromCache
import io.silv.database.AmadeusDatabase
import io.silv.database.dao.MangaDao
import io.silv.domain.manga.model.Manga
import io.silv.domain.manga.model.MangaUpdate
import io.silv.domain.manga.model.MangaWithChapters
import io.silv.domain.manga.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MangaRepositoryImpl(
    private val mangaDao: MangaDao,
    private val dispatchers: AmadeusDispatchers,
    private val database: AmadeusDatabase,
    private val coverCache: CoverCache,
): MangaRepository {

    override suspend fun getMangaById(id: String): Manga? =
        withContext(dispatchers.io) {
            mangaDao.getById(id)?.let(MangaMapper::mapManga)
        }

    override suspend fun updateManga(list: List<Manga>) =
        withContext(dispatchers.io) {
            list.forEach {
                mangaDao.update(MangaMapper.toEntity(it))
            }
        }

    override suspend fun updateManga(manga: Manga) =
        withContext(dispatchers.io) {
            mangaDao.update(MangaMapper.toEntity(manga))
        }

    override suspend fun upsertManga(update: MangaUpdate) =
        withContext(dispatchers.io) {
            mangaDao.upsert(
                id = update.id,
                coverArt = update.coverArt,
                title = update.title,
                version = update.version,
                updatedAt = update.updatedAt,
                description = update.description,
                alternateTitles = update.alternateTitles,
                originalLanguage = update.originalLanguage,
                availableTranslatedLanguages = update.availableTranslatedLanguages,
                status = update.status,
                tagToId = update.tagToId,
                contentRating = update.contentRating,
                lastVolume = update.lastVolume,
                lastChapter = update.lastChapter,
                publicationDemographic = update.publicationDemographic,
                savedAtLocal = update.savedAtLocal,
                year = update.year,
                latestUploadedChapter = update.latestUploadedChapter,
                authors = update.authors,
                artists = update.artists,
                createdAt = update.createdAt,
                progressState = update.progressState,
                favorite = update.favorite,
                readingStatus = update.readingStatus,
            ) { existing ->
                existing.deleteOldCoverFromCache(coverCache, update)
            }
        }

    override suspend fun upsertManga(updates: List<MangaUpdate>, withTransaction: Boolean) =
        withContext(dispatchers.io) {
            if (withTransaction) {
                database.withTransaction {
                    updates.forEach { upsertManga(it) }
                }
            } else {
                updates.forEach { upsertManga(it) }
            }
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
