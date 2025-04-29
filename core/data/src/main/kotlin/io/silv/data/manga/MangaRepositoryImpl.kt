package io.silv.data.manga

import androidx.room.withTransaction
import io.silv.common.AmadeusDispatchers
import io.silv.common.time.epochSeconds
import io.silv.data.chapter.ChapterMapper
import io.silv.data.download.CoverCache
import io.silv.data.util.deleteOldCoverFromCache
import io.silv.database.AmadeusDatabase
import io.silv.database.dao.MangaDao
import io.silv.data.manga.model.Manga
import io.silv.data.manga.model.MangaUpdate
import io.silv.data.manga.model.MangaWithChapters
import io.silv.data.manga.model.toUpdate
import io.silv.data.manga.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime

internal class MangaRepositoryImpl internal constructor(
    private val mangaDao: MangaDao,
    private val dispatchers: AmadeusDispatchers,
    private val database: AmadeusDatabase,
    private val coverCache: CoverCache,
) : MangaRepository {

    override suspend fun getMangaById(id: String): Manga? =
        withContext(dispatchers.io) {
            mangaDao.getById(id)?.let(MangaMapper::mapManga)
        }

    override suspend fun updateManga(update: MangaUpdate) =
        withContext(dispatchers.io) {
            with(update) {
                mangaDao.updateFields(
                    mangaId = id,
                    coverArt = coverArt,
                    title = title,
                    version = version,
                    updatedAt = updatedAt,
                    description = description,
                    alternateTitles = alternateTitles,
                    originalLanguage = originalLanguage,
                    availableTranslatedLanguages = availableTranslatedLanguages,
                    status = status,
                    tagToId = tagToId,
                    contentRating = contentRating,
                    lastVolume = lastVolume,
                    lastChapter = lastChapter,
                    publicationDemographic = publicationDemographic,
                    year = year,
                    latestUploadedChapter = latestUploadedChapter,
                    authors = authors,
                    artists = artists,
                    coverLastModified = coverLastModified
                )
            }
        }

    override fun observeLastLibrarySynced(): Flow<LocalDateTime?> {
        return mangaDao.observeLastSyncedTime()
    }

    override suspend fun getMangaByTitle(title: String): Manga? =
        withContext(dispatchers.io) { mangaDao.getMangaByTitle(title)?.let(MangaMapper::mapManga) }

    override suspend fun insertManga(manga: Manga) =
        withContext(dispatchers.io) {
            mangaDao.upsert(
                MangaMapper.toEntity(manga)
            ) { existing ->
                val deleted = existing.deleteOldCoverFromCache(coverCache, manga.toUpdate())
                if (deleted) epochSeconds() else null
            }
        }

    override suspend fun insertManga(manga: List<Manga>, withTransaction: Boolean) {
        withContext(dispatchers.io) {
            if (withTransaction) {
                database.withTransaction {
                    manga.forEach { insertManga(it) }
                }
            } else {
                manga.forEach { insertManga(it) }
            }
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

    override suspend fun getLibraryManga(): List<Manga> =
        withContext(dispatchers.io) {
            mangaDao.getLibraryManga().map(MangaMapper::mapManga)
        }

    override fun observeLibraryMangaWithChapters(): Flow<List<MangaWithChapters>> {
        return mangaDao.observeLibraryMangaWithChapters().map {
            it.map(MangaMapper::mapMangaWithChapters)
        }
    }

    override suspend fun getLibraryMangaWithChapters(): List<MangaWithChapters> =
        withContext(dispatchers.io) {
            mangaDao.getLibraryMangaWithChapters().map(MangaMapper::mapMangaWithChapters)
        }

    override fun observeMangaWithChaptersById(id: String): Flow<MangaWithChapters> {
        return mangaDao.observeMangaWithChaptersById(id).map { (manga, chapters) ->
            MangaWithChapters(
                MangaMapper.mapManga(manga),
                chapters.map(ChapterMapper::mapChapter)
            )
        }
    }

    override suspend fun getMangaWithChaptersById(id: String): MangaWithChapters? {
        return mangaDao.getMangaWithChaptersById(id)?.let { (manga, chapters) ->
            MangaWithChapters(
                MangaMapper.mapManga(manga),
                chapters.map(ChapterMapper::mapChapter)
            )
        }
    }

    override suspend fun deleteUnused() =
        withContext(dispatchers.io) { mangaDao.deleteUnused() }

    override fun observeUnusedCount(): Flow<Int> {
        return mangaDao.observeUnusedMangaCount()
    }
}
