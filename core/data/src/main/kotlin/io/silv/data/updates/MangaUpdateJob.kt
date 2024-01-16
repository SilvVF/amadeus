package io.silv.data.updates

import androidx.room.withTransaction
import io.silv.common.model.Status
import io.silv.common.time.epochMillis
import io.silv.common.time.localDateTimeNow
import io.silv.data.mappers.toChapterEntity
import io.silv.data.util.GetChapterList
import io.silv.database.AmadeusDatabase
import io.silv.database.dao.ChapterDao
import io.silv.database.dao.MangaDao
import io.silv.database.entity.manga.MangaEntity
import kotlin.time.Duration.Companion.minutes

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
            else -> {
                localDateTimeNow().epochMillis() -
                        mangaEntity.lastSyncedForUpdates!!.epochMillis() > 5.minutes.inWholeMilliseconds
            }
        }
    }

    suspend fun update(forceUpdate: Boolean) {
        database.withTransaction {
            val favorites = mangaDao.getLibraryManga()

            for (manga in favorites) {

                if (!forceUpdate && !shouldCheckForUpdates(manga)) { continue }

                val updatedChapterList = getChapterList.await(manga.id)

                val prevList = chapterDao.getChaptersByMangaId(manga.id)

                for (chapter in updatedChapterList) {

                    val prev = prevList.find { p -> p.id == chapter.id }

                    if (prev != null) {
                        chapterDao.updateChapter(chapter.toChapterEntity(prev))
                    } else {
                        chapterDao.upsertChapter(chapter.toChapterEntity())
                    }
                }
                val newIds = updatedChapterList.map { it.id }

                for (unhandled in prevList.filter { prev -> prev.id !in newIds }) {
                    chapterDao.deleteChapter(unhandled)
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