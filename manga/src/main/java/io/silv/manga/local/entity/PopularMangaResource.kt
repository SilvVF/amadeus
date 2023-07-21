package io.silv.manga.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.Status
import kotlinx.datetime.Clock

@Entity
data class PopularMangaResource(

    @PrimaryKey override val id: String,

    override val coverArt: String,

    override val description: String,

    override val titleEnglish: String,

    override val alternateTitles: Map<String, String>,

    override val originalLanguage: String,

    override val availableTranslatedLanguages: List<String>,

    override val status: Status,

    override val contentRating: ContentRating,

    override val lastVolume: String? = null,

    override val lastChapter: String? = null,

    override val version: Int,

    override val createdAt: String,

    override val updatedAt: String,

    override val volumeToCoverArt: Map<String, String> = emptyMap(),

    override val savedLocalAtEpochSeconds: Long = Clock.System.now().epochSeconds
): MangaResource, AmadeusEntity {

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