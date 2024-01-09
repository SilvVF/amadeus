package io.silv.data.history

import io.silv.common.AmadeusDispatchers
import io.silv.database.dao.HistoryDao
import io.silv.domain.history.HistoryRepository
import io.silv.model.History
import io.silv.model.HistoryUpdate
import io.silv.model.HistoryWithRelations
import kotlinx.coroutines.withContext

class HistoryRepositoryImpl(
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

    override suspend fun getHistory(): List<HistoryWithRelations> =
        withContext(dispatchers.io){
            historyDao.history().map {
                HistoryWithRelations(
                    id = it.id,
                    chapterId = it.chapterId,
                    lastRead = it.lastRead,
                    timeRead = it.timeRead,
                    mangaId = it.mangaId,
                    coverArt = it.coverArt
                )
            }
        }
}