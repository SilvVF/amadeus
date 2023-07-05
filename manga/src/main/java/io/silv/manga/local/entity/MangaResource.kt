package io.silv.manga.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.Status
import io.silv.manga.sync.AmadeusEntity
import kotlinx.datetime.Clock

@Entity
data class MangaResource(

    @PrimaryKey override val id: String,

    val coverArt: String,

    val description: String,

    val titleEnglish: String,

    val alternateTitles: Map<String, String>,

    val originalLanguage: String,

    val availableTranslatedLanguages: List<String>,

    val status: Status,

    val contentRating: ContentRating,

    val lastVolume: String? = null,

    val lastChapter: String? = null,

    val version: Int,

    val createdAt: String,

    val updatedAt: String,

    val volumeToCoverArt: Map<String, String> = emptyMap(),

    val savedLocalAtEpochSeconds: Long = Clock.System.now().epochSeconds
): AmadeusEntity {

    constructor(savedMangaEntity: SavedMangaEntity): this(
        id = savedMangaEntity.id,
        coverArt = savedMangaEntity.coverArt,
        description = savedMangaEntity.description,
        titleEnglish = savedMangaEntity.titleEnglish,
        alternateTitles = savedMangaEntity.alternateTitles,
        originalLanguage = savedMangaEntity.originalLanguage,
        availableTranslatedLanguages = savedMangaEntity.availableTranslatedLanguages,
        status = savedMangaEntity.status,
        contentRating = savedMangaEntity.contentRating,
        lastVolume = savedMangaEntity.lastVolume,
        lastChapter = savedMangaEntity.lastChapter,
        version = savedMangaEntity.version,
        createdAt = savedMangaEntity.createdAt,
        updatedAt = savedMangaEntity.updatedAt,
        volumeToCoverArt = savedMangaEntity.volumeToCoverArt,
        savedLocalAtEpochSeconds = savedMangaEntity.savedLocalAtEpochSeconds
    )
}
