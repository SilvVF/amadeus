package io.silv.domain.history

import kotlinx.datetime.LocalDateTime

data class HistoryUpdate(
    val chapterId: String,
    val readAt: LocalDateTime,
    val sessionReadDuration: Long,
)