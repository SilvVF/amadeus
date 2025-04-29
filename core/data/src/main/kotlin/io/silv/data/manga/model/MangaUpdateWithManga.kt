package io.silv.data.manga.model

import io.silv.common.model.UpdateType
import kotlinx.datetime.LocalDateTime

data class MangaUpdateWithManga(
    val id: Long,
    val savedMangaId: String,
    val updateType: UpdateType,
    val createdAt: LocalDateTime,
    val manga: Manga
)