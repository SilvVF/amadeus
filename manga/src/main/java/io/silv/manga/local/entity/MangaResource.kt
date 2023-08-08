package io.silv.manga.local.entity

import androidx.room.Entity
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.PublicationDemographic
import io.silv.manga.network.mangadex.models.Status
import kotlinx.datetime.LocalDateTime

@Entity
interface MangaResource {
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
