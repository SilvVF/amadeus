package io.silv.manga.repositorys.manga

import io.silv.core.AmadeusDispatchers
import io.silv.manga.local.dao.MangaUpdateDao
import io.silv.manga.local.entity.MangaUpdateEntity
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.local.entity.UpdateType
import io.silv.manga.local.entity.relations.MangaUpdateEntityWithManga
import io.silv.manga.network.mangadex.models.Status
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.repositorys.minus
import io.silv.manga.repositorys.timeNow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import kotlin.time.toKotlinDuration

class MangaUpdateRepositoryImpl(
    private val dispatchers: AmadeusDispatchers,
    private val mangaUpdateDao: MangaUpdateDao,
): MangaUpdateRepository {

    init {
        CoroutineScope(dispatchers.io).launch {
            for (update in mangaUpdateDao.getAllUpdates()) {
                if(timeNow() - update.createdAt >= Duration.ofHours(24).toKotlinDuration()) {
                    mangaUpdateDao.delete(update)
                }
            }
        }
    }

    override fun observeAllUpdates(): Flow<List<MangaUpdateEntityWithManga>> {
        return mangaUpdateDao.observeAllUpdatesWithManga()
    }

    override suspend fun createUpdate(
        prev: SavedMangaEntity,
        new: Manga
    ) = withContext(dispatchers.io) {
        if (
            (prev.status != Status.completed && prev.status != Status.cancelled) &&
            (new.attributes.latestUploadedChapter != prev.latestUploadedChapter
                    || (new.attributes.lastChapter?.toLongOrNull() ?: -1L) != prev.lastChapter)
        ) {
            mangaUpdateDao.upsert(
                MangaUpdateEntity(
                    savedMangaId = prev.id,
                    updateType = when {
                        (new.attributes.lastVolume?.toIntOrNull() ?: -1) != prev.lastVolume -> UpdateType.Volume
                        new.attributes.latestUploadedChapter != prev.latestUploadedChapter -> UpdateType.Chapter
                        (new.attributes.lastChapter?.toLongOrNull() ?: -1L) != prev.lastChapter -> UpdateType.Chapter
                        else -> UpdateType.Other
                    }
                )
            )
        }
    }
}