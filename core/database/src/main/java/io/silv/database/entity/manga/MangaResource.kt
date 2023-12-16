package io.silv.database.entity.manga

import androidx.room.Entity
import io.silv.common.model.ContentRating
import io.silv.common.model.ProgressState
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.ReadingStatus
import io.silv.common.model.Status
import kotlinx.datetime.LocalDateTime

@Entity
interface MangaResource {
    val progressState: ProgressState
    val readingStatus: ReadingStatus
    val id: String
    val coverArt: String
    val description: String
    val year: Int
        get() = 0
    val titleEnglish: String
    val alternateTitles: Map<String, String>
    val originalLanguage: String
    val availableTranslatedLanguages: List<String>
    val tagToId: Map<String, String>
    val status: Status
    val contentRating: ContentRating
    val lastVolume: Int
        get() = -1
    val lastChapter: Long
        get() = -1L
    val version: Int
    val latestUploadedChapter: String?
    val authors: List<String>
    val artists: List<String>
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
    val volumeToCoverArt: Map<String, String>
    val savedAtLocal: LocalDateTime
    val publicationDemographic: PublicationDemographic?
}
