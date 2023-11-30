package io.silv.data.manga

import io.silv.common.AmadeusDispatchers
import io.silv.common.model.Status
import io.silv.common.model.UpdateType
import io.silv.common.time.localDateTimeNow
import io.silv.common.time.minus
import io.silv.database.dao.MangaUpdateDao
import io.silv.database.entity.manga.MangaUpdateEntity
import io.silv.database.entity.manga.SavedMangaEntity
import io.silv.database.entity.relations.MangaUpdateEntityWithManga
import io.silv.network.model.manga.Manga
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.hours

class MangaUpdateRepositoryImpl(
    private val dispatchers: AmadeusDispatchers,
    private val mangaUpdateDao: MangaUpdateDao,
): MangaUpdateRepository {

    init {
        CoroutineScope(dispatchers.io).launch {
            for (update in mangaUpdateDao.getAllUpdates()) {
                if(localDateTimeNow() - update.createdAt >= 24.hours) {
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
            (new.attributes.latestUploadedChapter != prev.latestUploadedChapter ||
            (new.attributes.lastChapter?.toLongOrNull() ?: -1L) != prev.lastChapter)
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