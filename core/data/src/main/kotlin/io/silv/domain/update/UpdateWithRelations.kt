package io.silv.domain.update

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime

@Stable
data class UpdateWithRelations(
    val mangaId: String,
    val mangaTitle: String,
    val chapterId: String,
    val chapterName: String,
    val scanlator: String?,
    val read: Boolean,
    val bookmark: Boolean,
    val lastPageRead: Long,
    val favorite: Boolean,
    val coverArt: String,
    val coverLastModified: Long,
    val chapterNumber: Double,
    val savedAtLocal: LocalDateTime,
    val chapterUpdatedAt: LocalDateTime,
)