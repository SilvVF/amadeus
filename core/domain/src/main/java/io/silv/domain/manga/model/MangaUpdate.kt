package io.silv.domain.manga.model

import io.silv.common.model.ContentRating
import io.silv.common.model.ProgressState
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.ReadingStatus
import io.silv.common.model.Status
import kotlinx.datetime.LocalDateTime

data class MangaUpdate(
    val id: String,
    val coverArt: String,
    val  title: String,
    val version: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val favorite: Boolean?,
    val description: String,
    val alternateTitles: Map<String, String>,
    val originalLanguage: String,
    val availableTranslatedLanguages: List<String>,
    val status: Status,
    val tagToId: Map<String, String>,
    val contentRating: ContentRating,
    val lastVolume: Int,
    val lastChapter: Long,
    val publicationDemographic: PublicationDemographic?,
    val year: Int,
    val latestUploadedChapter: String?,
    val authors: List<String>,
    val artists: List<String>,
    val progressState: ProgressState? = null,
    val readingStatus: ReadingStatus? = null,
    val coverLastModified: Long? = null
)