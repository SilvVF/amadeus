package io.silv.data.history

import io.silv.model.History
import io.silv.model.HistoryWithRelations
import kotlinx.datetime.LocalDateTime

object HistoryMapper {

    fun mapHistory(
        id: Long,
        chapterId: String,
        lastRead: LocalDateTime,
        timeRead: Long,
    ): History = History(
        id = id,
        chapterId = chapterId,
        lastRead = lastRead,
        timeRead = timeRead,
    )

    fun mapHistoryWithRelations(
        historyId: Long,
        mangaId: String,
        chapterId: String,
        title: String,
        coverArt: String,
        isFavorite: Boolean,
        chapterNumber: Long,
        lastRead: LocalDateTime,
        timeRead: Long,
    ): HistoryWithRelations = HistoryWithRelations(
        id = historyId,
        chapterId = chapterId,
        mangaId = mangaId,
        lastRead = lastRead,
        timeRead = timeRead,
        coverArt = coverArt,
    )
}