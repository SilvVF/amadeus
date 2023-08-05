package io.silv.manga.local.entity

import androidx.room.Entity
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.PublicationDemographic
import io.silv.manga.network.mangadex.models.Status

@Entity
interface MangaResource {
    val id: String
    val coverArt: String
    val description: String
    val year: Int?
    val titleEnglish: String
    val alternateTitles: Map<String, String>
    val originalLanguage: String
    val availableTranslatedLanguages: List<String>
    val tagToId: Map<String, String>
    val status: Status
    val contentRating: ContentRating
    val lastVolume: String?
    val lastChapter: String?
    val version: Int
    val latestUploadedChapter: String?
    val authors: List<String>
    val artists: List<String>
    val createdAt: String
    val updatedAt: String
    val volumeToCoverArt: Map<String, String>
    val savedLocalAtEpochSeconds: Long
    val publicationDemographic: PublicationDemographic?
}
