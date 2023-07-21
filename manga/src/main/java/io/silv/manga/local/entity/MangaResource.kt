package io.silv.manga.local.entity

import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.Status

interface MangaResource {
    val id: String
    val coverArt: String
    val description: String
    val titleEnglish: String
    val alternateTitles: Map<String, String>
    val originalLanguage: String
    val availableTranslatedLanguages: List<String>
    val status: Status
    val contentRating: ContentRating
    val lastVolume: String?
    val lastChapter: String?
    val version: Int
    val createdAt: String
    val updatedAt: String
    val volumeToCoverArt: Map<String, String>
    val savedLocalAtEpochSeconds: Long
}
