package io.silv.data.updates

import androidx.room.withTransaction
import io.silv.common.model.Status
import io.silv.common.time.localDateTimeNow
import io.silv.data.mappers.toChapterEntity
import io.silv.data.util.GetChapterList
import io.silv.database.AmadeusDatabase
import io.silv.database.dao.ChapterDao
import io.silv.database.dao.MangaDao
import io.silv.database.entity.manga.MangaEntity

class MangaUpdateJob internal constructor(
    private val database: AmadeusDatabase,
    private val mangaDao: MangaDao,
    private val getChapterList: GetChapterList,
    private val chapterDao: ChapterDao
) {

    private fun shouldCheckForUpdates(mangaEntity: MangaEntity): Boolean {
        return when {
            mangaEntity.status == Status.completed -> false
            mangaEntity.lastSyncedForUpdates == null -> true
            else -> true
        }
    }

    suspend fun update(forceUpdate: Boolean) {
        database.withTransaction {
            val favorites = mangaDao.getLibraryManga()

            for (manga in favorites) {

                if (!forceUpdate && !shouldCheckForUpdates(manga)) { continue }

                val updatedChapterList = getChapterList.await(manga.id)

                for (chapter in updatedChapterList) {
                    chapterDao.updateOrInsert(chapter.toChapterEntity())
                }

                mangaDao.update(
                    manga.copy(
                        lastSyncedForUpdates = localDateTimeNow()
                    )
                )
            }
        }
    }
}