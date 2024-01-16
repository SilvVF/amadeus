package io.silv.domain.history

import kotlinx.coroutines.flow.Flow

interface HistoryRepository {

    suspend fun insertHistory(update: HistoryUpdate)

    suspend fun getHistoryByMangaId(id: String): List<History>

    fun getHistory(query: String): Flow<List<HistoryWithRelations>>

    suspend fun delete(id: Long)

    suspend fun deleteAllForManga(id: String)

    suspend fun clearHistory()
}