package io.silv.domain.history

import io.silv.model.History
import io.silv.model.HistoryUpdate
import io.silv.model.HistoryWithRelations

interface HistoryRepository {

    suspend fun insertHistory(update: HistoryUpdate)

    suspend fun getHistoryByMangaId(id: String): List<History>

    suspend fun getHistory(): List<HistoryWithRelations>
}