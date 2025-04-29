package io.silv.data.history

import kotlinx.datetime.LocalDateTime


data class History(
    val id: Long,
    val chapterId: String,
    val lastRead: LocalDateTime,
    val timeRead: Long
)

