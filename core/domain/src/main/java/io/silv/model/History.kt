package io.silv.model

import io.silv.common.model.MangaCover
import io.silv.domain.chapter.model.Chapter
import kotlinx.datetime.LocalDateTime

data class History(
    val id: Long,
    val chapterId: String,
    val lastRead: LocalDateTime,
    val timeRead: Int
)


data class HistoryWithRelations(
    val id: Long,
    val chapterId: String,
    val lastRead: LocalDateTime,
    val timeRead: Int,
    val chapter: Chapter,
    val mangaCover: MangaCover
)