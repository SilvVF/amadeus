package io.silv.model

import kotlinx.datetime.LocalDateTime


data class History(
    val id: Long,
    val chapterId: String,
    val lastRead: LocalDateTime,
    val timeRead: Long
)


data class HistoryWithRelations(
    val id: Long,
    val chapterId: String,
    val lastRead: LocalDateTime,
    val timeRead: Long,
    val mangaId: String,
    val coverArt: String
)

data class HistoryUpdate(
    val chapterId: String,
    val readAt: LocalDateTime,
    val sessionReadDuration: Long,
)