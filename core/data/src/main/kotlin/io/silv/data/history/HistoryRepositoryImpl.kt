package io.silv.data.history

import io.silv.common.AmadeusDispatchers
import io.silv.database.dao.HistoryDao
import io.silv.domain.history.History
import io.silv.domain.history.HistoryRepository
import io.silv.domain.history.HistoryUpdate
import io.silv.domain.history.HistoryWithRelations
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class HistoryRepositoryImpl(
    private val historyDao: HistoryDao,
    private val dispatchers: AmadeusDispatchers
): HistoryRepository {

    override suspend fun insertHistory(update: HistoryUpdate) =
        withContext(dispatchers.io) {
            historyDao.upsert(
                update.chapterId,
                update.readAt,
                update.sessionReadDuration
            )
        }

    override suspend fun getHistoryByMangaId(id: String): List<History> =
        withContext(dispatchers.io) {
            historyDao.getHistoryByMangaId(id).map {
                History(
                    it.id,
                    it.chapterId,
                    it.lastRead,
                    it.timeRead
                )
            }
        }

    override fun getHistory(query: String) =
            historyDao.history(query).map { list ->
                list.map {
                    HistoryWithRelations(
                        id = it.id,
                        chapterId = it.chapterId,
                        lastRead = it.lastRead,
                        timeRead = it.timeRead,
                        mangaId = it.mangaId,
                        coverArt = it.coverArt,
                        chapterName = it.name,
                        title = it.title,
                        lastPage = it.lastPageRead,
                        pageCount = it.pageCount,
                        chapterNumber = it.chapter,
                        volume = it.volume,
                        favorite = it.favorite,
                        coverLastModified = it.coverLastModified
                    )
                }
            }

    override suspend fun delete(id: Long) =
        withContext(dispatchers.io) { historyDao.delete(id) }

    override suspend fun deleteAllForManga(id: String) =
        withContext(dispatchers.io) { historyDao.deleteByMangaId(id) }

    override suspend fun clearHistory() =
        withContext(dispatchers.io) { historyDao.clear() }

    override suspend fun getTotalReadingTime(): Long = withContext(dispatchers.io){
        historyDao.getTotalReadingTime()
    }
}