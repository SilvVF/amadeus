package io.silv.data.manga

import io.silv.common.AmadeusDispatchers
import io.silv.common.model.Status
import io.silv.common.model.UpdateType
import io.silv.common.time.localDateTimeNow
import io.silv.common.time.minus
import io.silv.data.mappers.toEntity
import io.silv.database.dao.MangaUpdateDao
import io.silv.database.entity.manga.MangaUpdateEntity
import io.silv.domain.manga.interactor.GetManga
import io.silv.domain.manga.model.MangaUpdateWithManga
import io.silv.domain.manga.repository.MangaUpdateRepository
import io.silv.network.MangaDexApi
import io.silv.network.util.fetchMangaChunked
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.hours

class MangaUpdateRepositoryImpl(
    private val dispatchers: AmadeusDispatchers,
    private val mangaUpdateDao: MangaUpdateDao,
    private val mangaDexApi: MangaDexApi,
    private val getManga: GetManga,
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

    override fun observeAllUpdates(): Flow<List<MangaUpdateWithManga>> {
        return mangaUpdateDao.observeAllUpdatesWithManga().map {
            list -> list.map(MangaMapper::mapUpdateWithManga)
        }
    }

    override suspend fun createUpdates(
        mangaIds: List<String>
    ) = withContext(dispatchers.io) {

        val mangaList = mangaDexApi.fetchMangaChunked(mangaIds).map { it.toEntity() }

        for (new in mangaList) {
            val prev = getManga.await(new.id) ?: continue
            if (
                (prev.status != Status.completed && prev.status != Status.cancelled) &&
                (new.latestUploadedChapter != prev.latestUploadedChapter ||
                        (new.lastChapter) != prev.lastChapter)
            ) {
                mangaUpdateDao.upsert(
                    MangaUpdateEntity(
                        savedMangaId = prev.id,
                        updateType = when {
                            (new.lastVolume) != prev.lastVolume -> UpdateType.Volume
                            new.latestUploadedChapter != prev.latestUploadedChapter -> UpdateType.Chapter
                            else -> UpdateType.Chapter
                        }
                    )
                )
            }
        }
    }
}